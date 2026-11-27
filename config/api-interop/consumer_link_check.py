#!/usr/bin/env python3
"""Consumer link check: do the SDK's shipped consumers still link against the freshly built SDK?

Separately published artifacts (Signed Call, older push-templates / hms / geofence, Leanplum) are
compiled against an older CleverTap core and run against whatever core the app resolves. A change
that keeps the source API but changes the JVM shape (a `static` method moved into a Kotlin
`Companion`, a class made `final`, a field retyped) compiles fine here and crashes there with
NoSuchMethodError / IncompatibleClassChangeError / VerifyError / AbstractMethodError.

For every consumer class this resolves each Fieldref / Methodref / InterfaceMethodref and each
class reference into an SDK package against the built SDK classes, the way the JVM does (JVMS 5.4.3):

  NO_CLASS          referenced SDK class does not exist                    NoClassDefFoundError
  CLASS_ACCESS      referenced SDK class is not public                     IllegalAccessError
  NO_MEMBER         field/method not found in the class or its supertypes  NoSuchField/MethodError
  STATIC_MISMATCH   invokestatic/getstatic/putstatic on an instance member
                    (or the reverse)                                       IncompatibleClassChangeError
  OWNER_KIND        Methodref whose owner is now an interface, or
                    InterfaceMethodref whose owner is now a class          IncompatibleClassChangeError
  MEMBER_ACCESS     resolved member is private / package-private from
                    another package                                        IllegalAccessError
  FINAL_FIELD_WRITE putfield/putstatic on a final field of another class    IllegalAccessError
  EXTENDS_FINAL     consumer class extends a final SDK class               VerifyError
  SUPER_KIND        extends an interface / implements a class              IncompatibleClassChangeError
  OVERRIDES_FINAL   consumer method overrides a final SDK method           VerifyError
  MISSING_IMPL      concrete consumer class leaves an SDK abstract method
                    unimplemented                                          AbstractMethodError

Resolution walks only classes it knows (SDK + consumer). A chain that leaves them (android.*,
androidx.*, java.*) is "undecidable" and is not reported.

Usage:
  consumer_link_check.py --repo <sdk-root> --consumers config/api-interop/consumers.txt \
      --jars-dir <dir with downloaded artifacts> [--baseline config/api-interop/consumer_link_baseline.txt]
      [--allow-missing] [--sdk-classes <dir> ...] [--write-baseline]

  <jars-dir> holds <artifactId>-<version>.aar or .jar, as written by fetch_consumers.sh. Local runs
  (Maven Central is not reachable from every machine) point --jars-dir at pre-downloaded files.
  --sdk-classes overrides the SDK class dirs (default: the release Kotlin + javac outputs of the four
  modules under --repo). Exits 1 on any finding that is not in the baseline.

Only the Python 3 standard library is used, and consumer archives are read in memory (never
extracted or executed).
"""
import argparse
import glob
import io
import os
import re
import struct
import sys
import zipfile

SDK_MODULES = ['clevertap-core', 'clevertap-geofence', 'clevertap-hms', 'clevertap-pushtemplates']
# SDK packages, without generated R classes.
SDK_RE = re.compile(r'^com/clevertap/android/(sdk|pushtemplates|geofence|hms)/(?!(.*/)?R(\$|$))')
MODULE_PKG = {'clevertap-core': 'com/clevertap/android/sdk/', 'clevertap-geofence': 'com/clevertap/android/geofence/',
              'clevertap-hms': 'com/clevertap/android/hms/', 'clevertap-pushtemplates': 'com/clevertap/android/pushtemplates/'}

PUBLIC, PRIVATE, PROTECTED, STATIC, FINAL = 0x1, 0x2, 0x4, 0x8, 0x10
INTERFACE, ABSTRACT = 0x200, 0x400
OBJECT_METHODS = {'equals', 'hashCode', 'toString', 'getClass', 'wait', 'notify', 'notifyAll', 'clone', 'finalize'}

# ---------------------------------------------------------------------------------------------
# Class file parsing (JVMS 4)


class _R:
    def __init__(self, b):
        self.b, self.p = b, 0

    def u1(self):
        v = self.b[self.p]
        self.p += 1
        return v

    def u2(self):
        v = struct.unpack_from('>H', self.b, self.p)[0]
        self.p += 2
        return v

    def u4(self):
        v = struct.unpack_from('>I', self.b, self.p)[0]
        self.p += 4
        return v

    def raw(self, n):
        v = self.b[self.p:self.p + n]
        self.p += n
        return v


# Fixed instruction lengths (opcode included); tableswitch/lookupswitch/wide handled separately.
_LEN = [1] * 256
for _op in (0x10, 0x12, 0x15, 0x16, 0x17, 0x18, 0x19, 0x36, 0x37, 0x38, 0x39, 0x3a, 0xa9, 0xbc):
    _LEN[_op] = 2
for _op in (0x11, 0x13, 0x14, 0x84, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xbb, 0xbd, 0xc0, 0xc1,
            0xc6, 0xc7) + tuple(range(0x99, 0xa9)):
    _LEN[_op] = 3
for _op in (0xc5,):
    _LEN[_op] = 4
for _op in (0xb9, 0xba, 0xc8, 0xc9):
    _LEN[_op] = 5
# opcode -> (kind of constant it references, static?)
_REF_OPS = {0xb2: ('get', True), 0xb3: ('put', True), 0xb4: ('get', False), 0xb5: ('put', False),
            0xb6: ('invoke', False), 0xb7: ('special', False), 0xb8: ('invoke', True), 0xb9: ('invoke', False)}


def _code_refs(code, refs):
    """Collect (cp index, opcode) for every member-referencing instruction in a Code attribute."""
    r = _R(code)
    r.u2(); r.u2()
    n = r.u4()
    bc = r.raw(n)
    i = 0
    while i < n:
        op = bc[i]
        if op == 0xaa:  # tableswitch
            j = (i + 4) & ~3
            lo, hi = struct.unpack_from('>ii', bc, j + 4)
            i = j + 12 + 4 * (hi - lo + 1)
            continue
        if op == 0xab:  # lookupswitch
            j = (i + 4) & ~3
            npairs = struct.unpack_from('>i', bc, j + 4)[0]
            i = j + 8 + 8 * npairs
            continue
        if op == 0xc4:  # wide
            i += 6 if bc[i + 1] == 0x84 else 4
            continue
        if op in _REF_OPS:
            refs.append((struct.unpack_from('>H', bc, i + 1)[0], op))
        i += _LEN[op]


def parse_class(data):
    r = _R(data)
    if r.u4() != 0xCAFEBABE:
        raise ValueError('not a class file')
    r.u2(); r.u2()
    n = r.u2()
    cp = [None] * n
    i = 1
    while i < n:
        t = r.u1()
        if t == 1:
            cp[i] = ('utf8', r.raw(r.u2()).decode('utf-8', 'replace'))
        elif t in (3, 4):
            r.raw(4)
        elif t in (5, 6):
            r.raw(8)
            i += 1
        elif t == 7:
            cp[i] = ('class', r.u2())
        elif t == 8:
            r.u2()
        elif t in (9, 10, 11):
            cp[i] = ({9: 'F', 10: 'M', 11: 'IM'}[t], r.u2(), r.u2())
        elif t == 12:
            cp[i] = ('nat', r.u2(), r.u2())
        elif t == 15:
            r.u1(); r.u2()
        elif t == 16:
            r.u2()
        elif t in (17, 18):
            r.u2(); r.u2()
        elif t in (19, 20):
            r.u2()
        else:
            raise ValueError('bad constant tag %d' % t)
        i += 1

    def utf(k):
        return cp[k][1]

    def cls(k):
        return utf(cp[k][1]) if k else None

    acc = r.u2()
    name = cls(r.u2())
    sup = cls(r.u2())
    ifs = [cls(r.u2()) for _ in range(r.u2())]
    op_refs = []

    def members(collect_code):
        out = []
        for _ in range(r.u2()):
            a, nm, d = r.u2(), utf(r.u2()), utf(r.u2())
            for _ in range(r.u2()):
                an, ln = utf(r.u2()), r.u4()
                body = r.raw(ln)
                if collect_code and an == 'Code':
                    _code_refs(body, op_refs)
            out.append({'acc': a, 'name': nm, 'desc': d})
        return out

    fields = members(False)
    methods = members(True)
    inner_acc = None
    for _ in range(r.u2()):
        an, ln = utf(r.u2()), r.u4()
        body = r.raw(ln)
        if an == 'InnerClasses':
            k = struct.unpack_from('>H', body)[0]
            for j in range(k):
                ic, _oc, _nm, fl = struct.unpack_from('>HHHH', body, 2 + 8 * j)
                if cls(ic) == name:
                    inner_acc = fl

    def ref(k):
        e = cp[k]
        nat = cp[e[2]]
        return e[0], cls(e[1]), utf(nat[1]), utf(nat[2])

    insn_refs = []
    for k, op in op_refs:
        kind, owner, nm, d = ref(k)
        insn_refs.append((op, kind, owner, nm, d))
    class_refs = [utf(e[1]) for e in cp if e and e[0] == 'class']
    return {'acc': acc, 'name': name, 'sup': sup, 'ifs': ifs, 'fields': fields, 'methods': methods,
            'insn_refs': insn_refs, 'class_refs': class_refs, 'inner_acc': inner_acc}


def load_class_dirs(dirs):
    out = {}
    for d in dirs:
        for root, _, files in os.walk(d):
            for f in files:
                if f.endswith('.class') and f != 'module-info.class':
                    with open(os.path.join(root, f), 'rb') as fh:
                        c = parse_class(fh.read())
                    out.setdefault(c['name'], c)
    return out


def _load_jar_bytes(data, out):
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        for n in z.namelist():
            if n.endswith('.class') and not n.endswith('module-info.class') and not n.startswith('META-INF/'):
                c = parse_class(z.read(n))
                out.setdefault(c['name'], c)


def load_archive(path):
    """Classes of a .jar, or of an .aar's classes.jar + libs/*.jar."""
    out = {}
    with open(path, 'rb') as fh:
        data = fh.read()
    if path.endswith('.aar'):
        with zipfile.ZipFile(io.BytesIO(data)) as z:
            for n in z.namelist():
                if n == 'classes.jar' or (n.startswith('libs/') and n.endswith('.jar')):
                    _load_jar_bytes(z.read(n), out)
    else:
        _load_jar_bytes(data, out)
    return out

# ---------------------------------------------------------------------------------------------
# Resolution


def resolve_field(classes, owner, name, desc):
    """JVMS 5.4.3.2: the class, its superinterfaces, then its superclass. -> (status, member, holder)."""
    seen, external = set(), False

    def walk(c):
        nonlocal external
        if not c or c in seen:
            return None
        seen.add(c)
        k = classes.get(c)
        if k is None:
            if c != 'java/lang/Object':
                external = True
            return None
        for f in k['fields']:
            if f['name'] == name and f['desc'] == desc:
                return f, c
        for i in k['ifs']:
            hit = walk(i)
            if hit:
                return hit
        return walk(k['sup'])

    hit = walk(owner)
    if hit:
        return 'ok', hit[0], hit[1]
    return ('external' if external else 'missing'), None, None


def resolve_method(classes, owner, name, desc):
    """JVMS 5.4.3.3/4: the class chain first, then all superinterfaces. -> (status, member, holder)."""
    if name in ('<init>', '<clinit>'):  # constructors are not inherited
        k = classes.get(owner)
        for m in (k['methods'] if k else []):
            if m['name'] == name and m['desc'] == desc:
                return 'ok', m, owner
        return ('missing' if k else 'external'), None, None
    external = False
    c = owner
    chain = []
    while c:
        k = classes.get(c)
        if k is None:
            if c != 'java/lang/Object':
                external = True
            break
        chain.append(c)
        for m in k['methods']:
            if m['name'] == name and m['desc'] == desc:
                return 'ok', m, c
        c = k['sup']
    seen = set()
    stack = [i for c in chain for i in classes[c]['ifs']]
    while stack:
        c = stack.pop(0)
        if not c or c in seen:
            continue
        seen.add(c)
        k = classes.get(c)
        if k is None:
            external = True
            continue
        for m in k['methods']:
            if m['name'] == name and m['desc'] == desc and not (m['acc'] & (PRIVATE | STATIC)):
                return 'ok', m, c
        stack += k['ifs']
    if name in OBJECT_METHODS:
        return 'external', None, None
    return ('external' if external else 'missing'), None, None


def package(name):
    return name.rsplit('/', 1)[0] if '/' in name else ''


def is_subclass(classes, c, target):
    seen = set()
    while c and c not in seen:
        if c == target:
            return True
        seen.add(c)
        k = classes.get(c)
        if k is None:
            return None  # undecidable
        c = k['sup']
    return False


def collect_methods(classes, cname, seen=None):
    """(name, desc) -> (holder, method) for cname and every supertype it can see (subclass wins)."""
    if seen is None:
        seen = set()
    out = {}
    if not cname or cname in seen or cname not in classes:
        return out
    seen.add(cname)
    k = classes[cname]
    for s in [k['sup']] + k['ifs']:
        for key, v in collect_methods(classes, s, seen).items():
            out.setdefault(key, v)
    for m in k['methods']:
        out[(m['name'], m['desc'])] = (cname, m)
    return out


def check(sdk, consumer):
    """Findings as (consumer class, subject, kind) for one consumer artifact."""
    # Consumer classes win over SDK classes with the same name (an older module shipping its own copy).
    classes = dict(sdk)
    classes.update(consumer)
    findings = set()

    def sdk_owned(n):
        return bool(n) and SDK_RE.match(n) and n not in consumer

    for cname, c in consumer.items():
        for raw in c['class_refs']:
            n = raw.lstrip('[')
            if n.startswith('L'):
                n = n[1:].rstrip(';')
            if not sdk_owned(n):
                continue
            k = sdk.get(n)
            if k is None:
                findings.add((cname, 'class ' + n, 'NO_CLASS'))
            elif not ((k['inner_acc'] if k['inner_acc'] is not None else k['acc']) & (PUBLIC | PROTECTED)) \
                    and package(n) != package(cname):
                findings.add((cname, 'class ' + n, 'CLASS_ACCESS'))

        for op, kind, owner, name, desc in c['insn_refs']:
            if not owner or owner.startswith('[') or not sdk_owned(owner) or owner not in sdk:
                continue  # array clone(), consumer-own, or NO_CLASS already reported
            subject = '%s %s.%s%s' % (kind, owner, name, desc)
            field = kind == 'F'
            st, m, holder = (resolve_field if field else resolve_method)(classes, owner, name, desc)
            if st == 'missing':
                findings.add((cname, subject, 'NO_MEMBER'))
                continue
            if st != 'ok':
                continue
            ok_kind = classes[owner]['acc'] & INTERFACE
            if kind == 'M' and ok_kind:
                findings.add((cname, subject, 'OWNER_KIND'))
            if kind == 'IM' and not ok_kind:
                findings.add((cname, subject, 'OWNER_KIND'))
            op_kind, op_static = _REF_OPS[op]
            if op_kind != 'special' and bool(m['acc'] & STATIC) != op_static:
                findings.add((cname, subject, 'STATIC_MISMATCH'))
            if m['acc'] & PRIVATE and holder != cname:
                findings.add((cname, subject, 'MEMBER_ACCESS'))
            elif not (m['acc'] & (PUBLIC | PROTECTED | PRIVATE)) and package(holder) != package(cname):
                findings.add((cname, subject, 'MEMBER_ACCESS'))
            elif m['acc'] & PROTECTED and package(holder) != package(cname) \
                    and is_subclass(classes, cname, holder) is False:
                findings.add((cname, subject, 'MEMBER_ACCESS'))
            if field and op_kind == 'put' and m['acc'] & FINAL and holder != cname:
                findings.add((cname, subject, 'FINAL_FIELD_WRITE'))

        sup = c['sup']
        if sdk_owned(sup) and sup in sdk:
            if sdk[sup]['acc'] & INTERFACE:
                findings.add((cname, 'extends ' + sup, 'SUPER_KIND'))
            elif sdk[sup]['acc'] & FINAL:
                findings.add((cname, 'extends ' + sup, 'EXTENDS_FINAL'))
        for i in c['ifs']:
            if sdk_owned(i) and i in sdk and not (sdk[i]['acc'] & INTERFACE):
                findings.add((cname, 'implements ' + i, 'SUPER_KIND'))

        supers = [s for s in [sup] + c['ifs'] if s]
        if not any(sdk_owned(s) for s in supers) and not any(s in consumer for s in supers):
            continue
        inherited = {}
        for s in supers:
            for key, v in collect_methods(classes, s).items():
                inherited.setdefault(key, v)
        for m in c['methods']:
            key = (m['name'], m['desc'])
            if key[0] in ('<init>', '<clinit>') or key not in inherited:
                continue
            h, om = inherited[key]
            if sdk_owned(h) and om['acc'] & FINAL and not (om['acc'] & (STATIC | PRIVATE)) \
                    and not (m['acc'] & (STATIC | PRIVATE)):
                findings.add((cname, 'override %s.%s%s' % (h, key[0], key[1]), 'OVERRIDES_FINAL'))
        if not (c['acc'] & (ABSTRACT | INTERFACE)):
            full = collect_methods(classes, cname)
            for key, (h, m) in full.items():
                if sdk_owned(h) and m['acc'] & ABSTRACT:
                    findings.add((cname, 'missing impl %s.%s%s' % (h, key[0], key[1]), 'MISSING_IMPL'))
    return findings

# ---------------------------------------------------------------------------------------------
# Driver


def read_consumers(path):
    """Lines: <group>:<artifact>:<version> <aar|jar> [replaces=<sdk module>]"""
    out = []
    for line in open(path):
        line = line.split('#', 1)[0].strip()
        if not line:
            continue
        parts = line.split()
        g, a, v = parts[0].split(':')
        opts = dict(p.split('=', 1) for p in parts[2:])
        out.append({'group': g, 'artifact': a, 'version': v, 'ext': parts[1], 'replaces': opts.get('replaces')})
    return out


def sdk_class_dirs(repo, modules):
    dirs = []
    for m in modules:
        dirs += [d for d in [os.path.join(repo, m, 'build/tmp/kotlin-classes/release')] if os.path.isdir(d)]
        dirs += glob.glob(os.path.join(repo, m, 'build/intermediates/javac/release/*/classes'))
    return dirs


def main():
    ap = argparse.ArgumentParser(description=__doc__.split('\n\n')[0])
    ap.add_argument('--repo', default='.')
    ap.add_argument('--consumers', default='config/api-interop/consumers.txt')
    ap.add_argument('--jars-dir', required=True, help='directory with <artifact>-<version>.aar|jar files')
    ap.add_argument('--baseline', default='config/api-interop/consumer_link_baseline.txt')
    ap.add_argument('--sdk-classes', nargs='*', help='override SDK class dirs (all modules)')
    ap.add_argument('--allow-missing', action='store_true',
                    help='skip consumers whose archive is not in --jars-dir (local runs) instead of failing')
    ap.add_argument('--write-baseline', action='store_true', help='rewrite the baseline with the current findings')
    args = ap.parse_args()

    consumers = read_consumers(os.path.join(args.repo, args.consumers) if not os.path.isabs(args.consumers)
                               else args.consumers)
    if args.sdk_classes:
        all_sdk = load_class_dirs(args.sdk_classes)
    else:
        dirs = sdk_class_dirs(args.repo, SDK_MODULES)
        if not dirs:
            sys.exit('No built SDK classes under %s - compile the release variants first' % args.repo)
        all_sdk = load_class_dirs(dirs)
    if not any(n.startswith('com/clevertap/android/sdk/') for n in all_sdk):
        sys.exit('clevertap-core classes not found')

    baseline_path = args.baseline if os.path.isabs(args.baseline) else os.path.join(args.repo, args.baseline)
    baseline = set()
    if os.path.exists(baseline_path):
        for line in open(baseline_path):
            line = line.strip()
            if line and not line.startswith('#'):
                baseline.add(line)

    current, missing_archives, checked = set(), [], 0
    for c in consumers:
        label = '%s:%s:%s' % (c['group'], c['artifact'], c['version'])
        path = os.path.join(args.jars_dir, '%s-%s.%s' % (c['artifact'], c['version'], c['ext']))
        if not os.path.exists(path):
            missing_archives.append(label)
            print('[%s] MISSING ARCHIVE %s' % (label, path))
            continue
        consumer = load_archive(path)
        sdk = all_sdk
        if c['replaces']:
            prefix = MODULE_PKG[c['replaces']]
            sdk = {k: v for k, v in all_sdk.items() if not k.startswith(prefix)}
        found = check(sdk, consumer)
        checked += 1
        refs = len({r[1:] for k in consumer.values() for r in k['insn_refs']
                    if r[2] and SDK_RE.match(r[2]) and r[2] not in consumer})
        print('[%s] classes=%d sdk-member-refs=%d findings=%d' % (label, len(consumer), refs, len(found)))
        for cls, subject, kind in sorted(found):
            key = '%s:%s | %s | %s | %s' % (c['group'], c['artifact'], cls, subject, kind)
            current.add(key)
            print('  %s %s | %s | %s' % ('baseline' if key in baseline else 'NEW     ', kind, cls, subject))

    if args.write_baseline:
        with open(baseline_path, 'w') as fh:
            fh.write('# Consumer link findings accepted as pre-existing (they fail on the pre-migration SDK too).\n')
            fh.write('# <group>:<artifact> | <consumer class> | <reference> | <kind>  - see consumer_link_check.py\n')
            for k in sorted(current):
                fh.write(k + '\n')
        print('wrote %d entries to %s' % (len(current), baseline_path))
        return 0

    new = sorted(current - baseline)
    stale = sorted(baseline - current)
    print('\nchecked %d consumer artifacts, %d findings, %d not in baseline' % (checked, len(current), len(new)))
    for s in stale:
        print('note: baseline entry no longer found: ' + s)
    failed = False
    if missing_archives and not args.allow_missing:
        print('FAIL: consumer archives missing: ' + ', '.join(missing_archives))
        failed = True
    if new:
        print('FAIL: new consumer link findings - a shipped artifact would crash against this SDK build:')
        for n in new:
            print('  ' + n)
        failed = True
    return 1 if failed else 0


if __name__ == '__main__':
    sys.exit(main())
