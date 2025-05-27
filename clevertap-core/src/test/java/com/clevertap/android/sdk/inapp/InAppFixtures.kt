package com.clevertap.android.sdk.inapp

object InAppFixtures {

    val TYPE_HALF_INTERSTITIAL = """
            {
              "type": "half-interstitial",
              "bg": "#ffffff",
              "tablet": false,
              "close": true,
              "message": {
                "text": "message from half inter",
                "color": "#434761",
                "replacements": "message from half inter",
                "og": ""
              },
              "title": {
                "text": "Hi from half inter",
                "color": "#434761",
                "replacements": "Hi from half inter",
                "og": ""
              },
              "buttons": [
                {
                  "text": "ButtonOne",
                  "color": "#000000",
                  "bg": "#1EB858",
                  "border": "#1EB858",
                  "radius": "4",
                  "actions": {
                    "close": true,
                    "type": "close",
                    "android": "",
                    "ios": "",
                    "kv": {
                      
                    }
                  }
                },
                {
                  "text": "ButtonTwo",
                  "color": "#000000",
                  "bg": "#1EB858",
                  "border": "#1EB858",
                  "radius": "4",
                  "actions": {
                    "close": true,
                    "type": "kv",
                    "android": "",
                    "ios": "",
                    "kv": {
                      "a": "aaa"
                    }
                  }
                }
              ],
              "media": {
                "url": "https://d2hduo7492lb76.cloudfront.net/dist/1631012864/i/06e2bfb81b9749cb8cc6b2a6712c68f0.jpeg?v=1742963502",
                "poster": "",
                "key": "06e2bfb81b9749cb8cc6b2a6712c68f0",
                "content_type": "image/jpeg",
                "filename": "",
                "processing": false
              },
              "isMediaSourceRecommended": false,
              "recommendedText": {
                "text": "",
                "replacements": "",
                "og": ""
              },
              "mediaLandscape": {
                
              },
              "isMediaSourceRecommendedLandscape": false,
              "recommendedTextLandscape": {
                "text": "",
                "replacements": "",
                "og": ""
              },
              "hasPortrait": true,
              "hasLandscape": false,
              "rounded-borders": false,
              "is_native": true,
              "ti": 1742963524,
              "wzrk_id": "1742963524_20250326",
              "wzrk_pivot": "wzrk_default",
              "priority": 5,
              "whenTriggers": [
                {
                  "eventName": "lalit"
                }
              ],
              "frequencyLimits": [
                
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                
              ],
              "wzrk_ttl": 1743136393
            }
        """.trimIndent()

    val TYPE_ADVANCED_BUILDER = """
        {
          "type": "custom-html",
          "deviceSettings": {
            "orientation": "portrait",
            "size": {
              "height": 926,
              "width": 428
            }
          },
          "w": {
            "dk": false,
            "sc": false,
            "pos": "c",
            "xp": 100,
            "yp": 100
          },
          "isJsEnabled": true,
          "is_native": true,
          "ti": 1742969683,
          "wzrk_id": "1742969683_20250326",
          "wzrk_pivot": "wzrk_default",
          "priority": 1,
          "whenTriggers": [
            {
              "eventName": "BlockBRTesting"
            }
          ],
          "frequencyLimits": [
            
          ],
          "excludeGlobalFCaps": -1,
          "occurrenceLimits": [
            
          ],
          "wzrk_ttl": 1743142761,
          "d": {
            "html": "some-html-block"
          }
        }
    """.trimIndent()

    val TYPE_ADVANCED_BUILDER_HEADER = """
        {
          "type": "custom-html",
          "deviceSettings": {
            "orientation": "portrait",
            "size": {
              "height": 926,
              "width": 428
            }
          },
          "w": {
            "dk": false,
            "sc": false,
            "pos": "t",
            "xp": 100,
            "yp": 100,
            "aspectRatio": 2
          },
          "isJsEnabled": true,
          "is_native": true,
          "ti": 1742969683,
          "wzrk_id": "1742969683_20250326",
          "wzrk_pivot": "wzrk_default",
          "priority": 1,
          "whenTriggers": [
            {
              "eventName": "event"
            }
          ],
          "frequencyLimits": [
            
          ],
          "excludeGlobalFCaps": -1,
          "occurrenceLimits": [
            
          ],
          "wzrk_ttl": 1743142761,
          "d": {
            "html": "some-html-block"
          }
        }
    """.trimIndent()

    val TYPE_ADVANCED_BUILDER_HEADER_LEGACY = """
        {
          "type": "custom-html",
          "deviceSettings": {
            "orientation": "portrait",
            "size": {
              "height": 926,
              "width": 428
            }
          },
          "w": {
            "dk": false,
            "sc": false,
            "pos": "t",
            "xp": 100,
            "yp": 25
          },
          "isJsEnabled": true,
          "is_native": true,
          "ti": 1742969683,
          "wzrk_id": "1742969683_20250326",
          "wzrk_pivot": "wzrk_default",
          "priority": 1,
          "whenTriggers": [
            {
              "eventName": "event"
            }
          ],
          "frequencyLimits": [
            
          ],
          "excludeGlobalFCaps": -1,
          "occurrenceLimits": [
            
          ],
          "wzrk_ttl": 1743142761,
          "d": {
            "html": "some-html-block"
          }
        }
    """.trimIndent()

    val TYPE_ADVANCED_BUILDER_FOOTER_LEGACY = """
        {
          "type": "custom-html",
          "deviceSettings": {
            "orientation": "portrait",
            "size": {
              "height": 926,
              "width": 428
            }
          },
          "w": {
            "dk": false,
            "sc": false,
            "pos": "b",
            "xp": 100,
            "yp": 20
          },
          "isJsEnabled": true,
          "is_native": true,
          "ti": 1742969683,
          "wzrk_id": "1742969683_20250326",
          "wzrk_pivot": "wzrk_default",
          "priority": 1,
          "whenTriggers": [
            {
              "eventName": "event"
            }
          ],
          "frequencyLimits": [
            
          ],
          "excludeGlobalFCaps": -1,
          "occurrenceLimits": [
            
          ],
          "wzrk_ttl": 1743142761,
          "d": {
            "html": "some-html-block"
          }
        }
    """.trimIndent()

    val TYPE_ADVANCED_BUILDER_FOOTER = """
        {
          "type": "custom-html",
          "deviceSettings": {
            "orientation": "portrait",
            "size": {
              "height": 926,
              "width": 428
            }
          },
          "w": {
            "dk": false,
            "sc": false,
            "pos": "b",
            "xp": 100,
            "yp": 100,
            "aspectRatio": 2
          },
          "isJsEnabled": true,
          "is_native": true,
          "ti": 1742969683,
          "wzrk_id": "1742969683_20250326",
          "wzrk_pivot": "wzrk_default",
          "priority": 1,
          "whenTriggers": [
            {
              "eventName": "event"
            }
          ],
          "frequencyLimits": [
            
          ],
          "excludeGlobalFCaps": -1,
          "occurrenceLimits": [
            
          ],
          "wzrk_ttl": 1743142761,
          "d": {
            "html": "some-html-block"
          }
        }
    """.trimIndent()

    val TYPE_ADVANCED_BUILDER_INVALID_ASPECT_RATIO = """
        {
          "type": "custom-html",
          "w": {
            "dk": false,
            "sc": false,
            "pos": "b",
            "xp": 100,
            "yp": 100,
            "aspectRatio": 0.0
          },
          "isJsEnabled": true,
          "is_native": true,
          "ti": 1742969683,
          "wzrk_id": "1742969683_20250326",
          "wzrk_pivot": "wzrk_default",
          "priority": 1,
          "whenTriggers": [
            {
              "eventName": "event"
            }
          ],
          "frequencyLimits": [
            
          ],
          "excludeGlobalFCaps": -1,
          "occurrenceLimits": [
            
          ],
          "wzrk_ttl": 1743142761,
          "d": {
            "html": "some-html-block"
          }
        }
    """.trimIndent()

    val TYPE_ADVANCED_BUILDER_COVER = """
        {
          "type": "custom-html",
          "w": {
            "dk": false,
            "sc": false,
            "pos": "b",
            "xp": 100,
            "yp": 100
          },
          "d": {
            "html": "some-html-block"
          }
        }
    """.trimIndent()

    val TYPE_ADVANCED_BUILDER_INTERSTITIAL = """
        {
          "type": "custom-html",
          "w": {
            "dk": false,
            "sc": false,
            "pos": "c",
            "xp": 90,
            "yp": 85
          },
          "d": {
            "html": "some-html-block"
          }
        }
    """.trimIndent()

    val TYPE_ADVANCED_BUILDER_HALF_INTERSTITIAL = """
        {
          "type": "custom-html",
          "w": {
            "dk": false,
            "sc": false,
            "pos": "c",
            "xp": 90,
            "yp": 50
          },
          "d": {
            "html": "some-html-block"
          }
        }
    """.trimIndent()

    val TYPE_BIG_HTML = """
        {
      "type": "custom-html",
      "deviceSettings": {
        "orientation": "portrait",
        "size": {
          "height": 840,
          "width": 420
        }
      },
      "w": {
        "dk": false,
        "sc": false,
        "pos": "c",
        "xp": 100,
        "yp": 100
      },
      "isJsEnabled": true,
      "is_native": true,
      "ti": 1736946682,
      "wzrk_id": "1736946682_20250129",
      "wzrk_pivot": "wzrk_default",
      "priority": 10,
      "whenTriggers": [
        {
          "eventName": "mtevent1"
        },
        {
          "eventName": "mtevent2"
        },
        {
          "eventName": "mt2"
        }
      ],
      "frequencyLimits": [
        {
          "type": "session",
          "limit": 3
        }
      ],
      "excludeGlobalFCaps": 1,
      "occurrenceLimits": [
        
      ],
      "wzrk_ttl_offset": 172800,
      "d": {
        "html": "<!doctype html><html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\"><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\" charset=\"utf-8\"><style>html,body,div,span,applet,object,iframe,h1,h2,h3,h4,h5,h6,p,blockquote,pre,a,abbr,acronym,address,big,cite,code,del,dfn,em,img,ins,kbd,q,s,samp,small,strike,strong,sub,sup,tt,var,b,u,i,center,dl,dt,dd,ol,ul,li,fieldset,form,label,legend,table,caption,tbody,tfoot,thead,tr,th,td,article,aside,canvas,details,embed,figure,figcaption,footer,header,hgroup,menu,nav,output,ruby,section,summary,time,mark,audio,video{font-size:100%;font:inherit;vertical-align:baseline;border:0;margin:0;padding:0}article,aside,details,figcaption,figure,footer,header,hgroup,menu,nav,section{display:block}body{line-height:1}ol,ul{list-style:none}blockquote,q{quotes:none}blockquote:before,blockquote:after,q:before,q:after{content:\"\";content:none}table{border-collapse:collapse;border-spacing:0}*{-webkit-tap-highlight-color:transparent;-webkit-touch-callout:none;-webkit-user-select:none;-ms-user-select:none;user-select:none}html{width:100%;height:100%}#cover{z-index:0;background:rgba(0,0,0,.5);width:100%;height:100%;position:absolute;top:0;left:0}.text{text-align:center}body{justify-content:center;align-items:center;width:100%;font-family:sans-serif;font-weight:400;display:flex;position:relative}#view{z-index:1;border:1px solid;flex:auto;position:relative}#view.rounded-border{border-radius:13px}#view.rounded-border #hero-image{border-top-left-radius:13px;border-top-right-radius:13px}#overlay{z-index:0;background-position:0 0;justify-content:center;align-items:center;width:100%;height:100%;display:flex;position:absolute;top:0;left:0;overflow:hidden}#content{z-index:1;background-position:50%;background-repeat:no-repeat;background-size:contain;justify-content:center;align-items:center;width:100%;height:100%;display:flex;position:relative}#backgroundImage{object-fit:contain;border-radius:inherit;max-width:100%;max-height:100%;position:absolute;top:50%;left:50%;transform:translate(-50%,-50%)}#elementsContainer{border-radius:inherit;width:100%;height:100%;display:block;position:relative;overflow:hidden}[id^=button-]{box-sizing:border-box;cursor:pointer;letter-spacing:0;background-color:transparent;justify-content:center;align-items:center;padding:0;font-family:Arial,sans-serif;line-height:1.2;position:absolute}[id^=image-]{position:absolute}[id^=text-]{-webkit-user-select:none;-ms-user-select:none;user-select:none;overflow-wrap:break-word;letter-spacing:0;font-family:Arial,sans-serif;line-height:1.2;position:absolute}#dismissButton{background:0 0;border-style:solid;justify-content:center;align-items:center;padding:1%;display:none;position:absolute}.lp-icon{justify-content:center;align-items:center;width:100%;min-width:20px;height:100%;min-height:100%;display:flex;position:relative}.icon-svg-container{height:100%}.icon-svg-container>svg{width:100%;height:100%;display:block}@keyframes fadeIn{0%{opacity:0}to{opacity:1}}@keyframes moveInRight{0%{transform:translate(-100cqw)}to{transform:translate(0)}}@keyframes moveInBottom{0%{transform:translateY(-100cqh)}to{transform:translateY(0)}}@keyframes moveInTop{0%{transform:translateY(100cqh)}to{transform:translateY(0)}}@keyframes moveInLeft{0%{transform:translate(100cqw)}to{transform:translate(0)}}</style></head><body> <div id=\"overlay\"> <div id=\"content\"> <img id=\"backgroundImage\" alt> <div id=\"elementsContainer\"> <button id=\"dismissButton\" role=\"button\" aria-label=\"Close\"> <i class=\"lp-icon\"> <i class=\"icon-svg-container\"> <svg viewBox=\"0 0 20 20\"><path d=\"M10 9.291 14.79 4.5a.5.5 0 1 1 .71.707L10.708 10l4.792 4.794a.5.5 0 0 1-.707.707L10 10.707 5.207 15.5a.5.5 0 0 1-.707-.707l4.792-4.794L4.5 5.207a.5.5 0 1 1 .71-.707L10 9.291Z\" fill=\"currentColor\"/></svg> </i> </i> </button> </div> </div> </div> <script>!function(e,t,r,n,o){var i=\"undefined\"!=typeof globalThis?globalThis:\"undefined\"!=typeof self?self:\"undefined\"!=typeof window?window:\"undefined\"!=typeof global?global:{},a=\"function\"==typeof i[n]&&i[n],s=a.cache||{},l=\"undefined\"!=typeof module&&\"function\"==typeof module.require&&module.require.bind(module);function c(t,r){if(!s[t]){if(!e[t]){var o=\"function\"==typeof i[n]&&i[n];if(!r&&o)return o(t,!0);if(a)return a(t,!0);if(l&&\"string\"==typeof t)return l(t);var u=Error(\"Cannot find module '\"+t+\"'\");throw u.code=\"MODULE_NOT_FOUND\",u}p.resolve=function(r){var n=e[t][1][r];return null!=n?n:r},p.cache={};var f=s[t]=new c.Module(t);e[t][0].call(f.exports,p,f,f.exports,this)}return s[t].exports;function p(e){var t=p.resolve(e);return!1===t?{}:c(t)}}c.isParcelRequire=!0,c.Module=function(e){this.id=e,this.bundle=c,this.exports={}},c.modules=e,c.cache=s,c.parent=a,c.register=function(t,r){e[t]=[function(e,t){t.exports=r},{}]},Object.defineProperty(c,\"root\",{get:function(){return i[n]}}),i[n]=c;for(var u=0;u<t.length;u++)c(t[u]);if(r){var f=c(r);\"object\"==typeof exports&&\"undefined\"!=typeof module?module.exports=f:\"function\"==typeof define&&define.amd?define(function(){return f}):o&&(this[o]=f)}}({h9QmU:[function(e,t,r){e(\"../ts/index.ts\")},{\"../ts/index.ts\":\"7UKRq\"}],\"7UKRq\":[function(e,t,r){var n=e(\"@swc/helpers/_/_async_to_generator\"),o=e(\"@swc/helpers/_/_sliced_to_array\"),i=e(\"@swc/helpers/_/_to_consumable_array\"),a=e(\"@swc/helpers/_/_ts_generator\"),s=e(\"./setup\"),l=e(\"./common\"),c=e(\"./types\"),u=[];function f(e){var t=(0,l.getDomElementById)(\"#backgroundImage\"),r=(0,l.getDomElementById)(\"#overlay\"),n=(0,l.getDomElementById)(\"#content\"),i=(0,l.getDomElementById)(\"#elementsContainer\"),a=\"\".concat(100-e.margin,\"%\");if(e.backgroundImage){if(0===t.clientWidth&&0===t.clientHeight)return;if(\"naturalWidth\"in t&&\"naturalHeight\"in t){var s=p({width:t.naturalWidth,height:t.naturalHeight},r);t.style.width=s?a:\"auto\",t.style.height=s?\"auto\":a}n.style.width=\"\".concat(t.clientWidth,\"px\"),n.style.height=\"\".concat(t.clientHeight,\"px\"),i.style.width=\"\".concat(t.clientWidth,\"px\"),i.style.height=\"\".concat(t.clientHeight,\"px\")}else{var c,f=(0,o._)(e.aspectRatio.split(\"/\").map(Number),2),d=f[0],y=f[1];p({width:d,height:y},r)?(c=y/d,n.style.width=a,n.style.height=\"\".concat(n.offsetWidth*c,\"px\")):(c=d/y,n.style.height=a,n.style.width=\"\".concat(n.offsetHeight*c,\"px\"))}u.forEach(function(e){return e()})}function p(e,t){return e.width/e.height>t.clientWidth/t.clientHeight}function d(e,t){e.style.width=\"\".concat(t.size.width,\"%\"),e.style.height=\"\".concat(t.size.height,\"%\"),e.style.top=\"\".concat(t.position.y,\"%\"),e.style.left=\"\".concat(t.position.x,\"%\"),e.style.zIndex=t.level?\"\".concat(t.level):\"0\"}function y(e,t,r){var n,o,i,a,s,l;u.push(function(){var n,o;t.style.fontSize=\"\".concat(e.clientHeight*(((null===(n=r.style)||void 0===n?void 0:null===(o=n.font)||void 0===o?void 0:o.size)||0)/1e3),\"px\")}),t.style.color=(null===(n=r.style)||void 0===n?void 0:null===(o=n.font)||void 0===o?void 0:o.color)||\"\",t.style.fontStyle=(null===(i=r.style)||void 0===i?void 0:null===(a=i.font)||void 0===a?void 0:a.style)||\"\",t.style.fontWeight=(null===(s=r.style)||void 0===s?void 0:null===(l=s.font)||void 0===l?void 0:l.weight)||\"\",t.style.fontFamily=\"inherit\"}function _(e,t,r,n){var o,i,a,s;u.push(function(){var o,i,a,s;t.style.borderWidth=\"\".concat(e.clientHeight*(((null===(o=r.style)||void 0===o?void 0:null===(i=o.border)||void 0===i?void 0:i.width)||0)/1e3),\"px\")||\"\",t.style.borderRadius=\"\".concat(e.clientHeight*(((null===(a=r.style)||void 0===a?void 0:null===(s=a.border)||void 0===s?void 0:s.radius)||0)/100),\"px\"),n&&(t.style.border=\"1px solid red\")}),t.style.borderStyle=(null===(o=r.style)||void 0===o?void 0:null===(i=o.border)||void 0===i?void 0:i.width)?\"solid\":\"none\",t.style.borderColor=(null===(a=r.style)||void 0===a?void 0:null===(s=a.border)||void 0===s?void 0:s.color)||\"\"}function m(e,t){e.style.backgroundColor=t.style.backgroundColor,t.style.activeBackgroundColor&&(e.addEventListener(\"mousedown\",function(){e.style.backgroundColor=t.style.activeBackgroundColor}),e.addEventListener(\"mouseup\",function(){e.style.backgroundColor=t.style.backgroundColor}),e.addEventListener(\"touchstart\",function(){e.style.backgroundColor=t.style.activeBackgroundColor}),e.addEventListener(\"touchend\",function(){e.style.backgroundColor=t.style.backgroundColor}))}var h=[\"Arial\",\"sans-serif\"];function v(e,t,r){if(r){var n=r.vOffset,o=r.hOffset,i=r.blur,a=r.color,s=\"\\n      \".concat(e.clientHeight*(o/1e3),\"px\\n      \").concat(e.clientHeight*(n/1e3),\"px\\n      \").concat(e.clientHeight*(i/1e3),\"px\\n      \").concat(a,\"\\n    \").replace(/\\s+/g,\" \").trim();t.style.boxShadow=s}}function b(){return(b=(0,n._)(function(e){return(0,a._)(this,function(t){switch(t.label){case 0:return[4,Promise.all(Object.entries(e).flatMap(function(e){var t,r=(0,o._)(e,2),n=r[0],i=r[1];return i&&\"object\"==typeof i?\"backgroundImage\"in i&&i.backgroundImage?[(0,l.preloadImage)(i.backgroundImage)]:\"font\"in i&&(null===(t=i.font)||void 0===t?void 0:t.fontUrl)?[(0,l.loadCustomFont)(i.font.fontUrl)]:\"images\"===n&&Array.isArray(i)?i.filter(function(e){return\"image\"in e}).map(function(e){return(0,l.preloadImage)(e.image)}):[]:[]}))];case 1:return t.sent(),[2]}})})).apply(this,arguments)}(0,s.runSetup)(function(e){return b.apply(this,arguments)},function(e){if(e.buttons.forEach(function(e){var t=(0,l.getDomElementById)(\"#\".concat(e.id));t&&(t.onclick=(0,l.runTrackedAction).bind(null,e))}),\"images\"in e&&e.images&&e.images.forEach(function(e){var t=(0,l.getDomElementById)(\"#\".concat(e.id));t&&(t.onclick=(0,l.runTrackedAction).bind(null,e))}),\"dismissButtonProps\"in e&&e.dismissButtonProps){var t=(0,l.getDomElementById)(\"#dismissButton\");t&&(t.onclick=(0,l.runTrackedAction).bind(null,e.dismissButtonProps))}},function(e){var t,r,n,o,a,s,p=e.variable,b=e.value,g=(0,l.getDomElementById)(\"#elementsContainer\"),w=(0,l.getDomElementById)(\"#content\");switch(p){case c.VariableTypes.overlay:return(t=(0,l.getDomElementById)(\"#overlay\")).style.backgroundColor=b.backgroundColor,b.backgroundImage&&(t.style.backgroundImage=\"url(\".concat(b.backgroundImage,\")\"),t.style.backgroundRepeat=b.backgroundRepeat,t.style.backgroundSize=b.backgroundSize),!0;case c.VariableTypes.content:return(r=(0,l.getDomElementById)(\"#backgroundImage\"))&&\"src\"in r&&b.backgroundImage?(r.src=b.backgroundImage,r.onload=r.onresize=f.bind(null,b)):(w.style.backgroundColor=b.backgroundColor?b.backgroundColor:\"\",window.onload=window.onresize=f.bind(null,b)),u.push(function(){w.style.borderRadius=\"\".concat(w.clientHeight*((b.borderRadius||0)/100),\"px\")}),n=h,\"font\"in b&&b.font&&(n=[\"'\".concat(b.font.fontFamily,\"'\")].concat((0,i._)(b.font.fallbackFonts),(0,i._)(n))),w.style.fontFamily=n.join(\", \"),function(e,t){if(\"animation\"in e&&e.animation){var r=\"none\";if(e.animation.type===c.AnimationTypes.DISSOLVE)r=\"fadeIn\";else if(e.animation.type===c.AnimationTypes.MOVE_IN)switch(e.animation.moveInDirection){case c.MoveInDirections.RIGHT:r=\"moveInRight\";break;case c.MoveInDirections.TOP:r=\"moveInTop\";break;case c.MoveInDirections.BOTTOM:r=\"moveInBottom\";break;default:r=\"moveInLeft\"}var n=e.animation.easing||c.EasingOptions.LINEAR;n===c.EasingOptions.CUBIC_BEZIER&&(n=\"cubic-bezier(\".concat(e.animation.bezier||\"0,0,0,0\",\")\")),t.style.animationName=r,t.style.animationTimingFunction=n,t.style.animationDuration=e.animation.duration?\"\".concat(e.animation.duration,\"ms\"):\"\"}}(b,w),!0;case c.VariableTypes.buttons:return!function(e,t,r){var n=e.find(function(e){return e.name.includes(\"{TEST}\")}),o=!0,i=!1,a=void 0;try{for(var s,l=e[Symbol.iterator]();!(o=(s=l.next()).done);o=!0)!function(){var e,o,i=s.value,a=document.createElement(\"button\");a.id=i.id,a.style.display=i.hidden?\"none\":\"flex\",a.innerText=i.text||\"\",d(a,i),y(t,a,i),_(t,a,i,n),v(t,a,null===(e=i.style)||void 0===e?void 0:e.boxShadow),u.push(function(){var e;v(t,a,null===(e=i.style)||void 0===e?void 0:e.boxShadow)}),(null===(o=i.style)||void 0===o?void 0:o.backgroundColor)&&m(a,i),r.appendChild(a)}()}catch(e){i=!0,a=e}finally{try{o||null==l.return||l.return()}finally{if(i)throw a}}}(b,w,g),!0;case c.VariableTypes.images:return!function(e,t){var r=!0,n=!1,o=void 0;try{for(var i,a=e[Symbol.iterator]();!(r=(i=a.next()).done);r=!0){var s=i.value,l=document.createElement(\"img\");l.id=s.id,d(l,s),l.src=s.image?s.image:\"\",t.appendChild(l)}}catch(e){n=!0,o=e}finally{try{r||null==a.return||a.return()}finally{if(n)throw o}}}(b,g),!0;case c.VariableTypes.textElements:return!function(e,t,r){var n=!0,o=!1,i=void 0;try{for(var a,s=e[Symbol.iterator]();!(n=(a=s.next()).done);n=!0){var l=a.value,c=document.createElement(\"div\");c.id=l.id,c.style.backgroundColor=l.style.backgroundColor,function(e,t){var r;e.style.textAlign=t.textAlign||\"\",e.innerText=(null===(r=t.text)||void 0===r?void 0:r.text)||\"\"}(c,l),d(c,l),y(t,c,l),_(t,c,l),r.appendChild(c)}}catch(e){o=!0,i=e}finally{try{n||null==s.return||s.return()}finally{if(o)throw i}}}(b,w,g),!0;case c.VariableTypes.dismissButtonProps:return(s=(0,l.getDomElementById)(\"#dismissButton\")).style.display=b.hidden?\"none\":\"flex\",d(s,b),u.push(function(){s.style.height=\"\".concat(s.offsetWidth,\"px\")}),_(w,s,b),s.style.stroke=(null===(o=b.style.icon)||void 0===o?void 0:o.strokeColor)||\"\",s.style.strokeWidth=\"\".concat(null===(a=b.style.icon)||void 0===a?void 0:a.strokeWidth,\"%\"),m(s,b),!0;default:return!1}})},{\"@swc/helpers/_/_async_to_generator\":\"eTGQ5\",\"@swc/helpers/_/_sliced_to_array\":\"jzEiA\",\"@swc/helpers/_/_to_consumable_array\":\"4Hwej\",\"@swc/helpers/_/_ts_generator\":\"hIXl2\",\"./setup\":\"eKFSM\",\"./common\":\"h1yCa\",\"./types\":\"eA9ym\"}],eTGQ5:[function(e,t,r){var n=e(\"@parcel/transformer-js/src/esmodule-helpers.js\");function o(e,t,r,n,o,i,a){try{var s=e[i](a),l=s.value}catch(e){r(e);return}s.done?t(l):Promise.resolve(l).then(n,o)}function i(e){return function(){var t=this,r=arguments;return new Promise(function(n,i){var a=e.apply(t,r);function s(e){o(a,n,i,s,l,\"next\",e)}function l(e){o(a,n,i,s,l,\"throw\",e)}s(void 0)})}}n.defineInteropFlag(r),n.export(r,\"_async_to_generator\",function(){return i}),n.export(r,\"_\",function(){return i})},{\"@parcel/transformer-js/src/esmodule-helpers.js\":\"3ieD1\"}],\"3ieD1\":[function(e,t,r){r.interopDefault=function(e){return e&&e.__esModule?e:{default:e}},r.defineInteropFlag=function(e){Object.defineProperty(e,\"__esModule\",{value:!0})},r.exportAll=function(e,t){return Object.keys(e).forEach(function(r){\"default\"===r||\"__esModule\"===r||t.hasOwnProperty(r)||Object.defineProperty(t,r,{enumerable:!0,get:function(){return e[r]}})}),t},r.export=function(e,t,r){Object.defineProperty(e,t,{enumerable:!0,get:r})}},{}],jzEiA:[function(e,t,r){var n=e(\"@parcel/transformer-js/src/esmodule-helpers.js\");n.defineInteropFlag(r),n.export(r,\"_sliced_to_array\",function(){return l}),n.export(r,\"_\",function(){return l});var o=e(\"./_array_with_holes.js\"),i=e(\"./_iterable_to_array_limit.js\"),a=e(\"./_non_iterable_rest.js\"),s=e(\"./_unsupported_iterable_to_array.js\");function l(e,t){return(0,o._array_with_holes)(e)||(0,i._iterable_to_array_limit)(e,t)||(0,s._unsupported_iterable_to_array)(e,t)||(0,a._non_iterable_rest)()}},{\"./_array_with_holes.js\":\"oCaes\",\"./_iterable_to_array_limit.js\":\"eiiS5\",\"./_non_iterable_rest.js\":\"69769\",\"./_unsupported_iterable_to_array.js\":\"llvDp\",\"@parcel/transformer-js/src/esmodule-helpers.js\":\"3ieD1\"}],oCaes:[function(e,t,r){var n=e(\"@parcel/transformer-js/src/esmodule-helpers.js\");function o(e){if(Array.isArray(e))return e}n.defineInteropFlag(r),n.export(r,\"_array_with_holes\",function(){return o}),n.export(r,\"_\",function(){return o})},{\"@parcel/transformer-js/src/esmodule-helpers.js\":\"3ieD1\"}],eiiS5:[function(e,t,r){var n=e(\"@parcel/transformer-js/src/esmodule-helpers.js\");function o(e,t){var r,n,o=null==e?null:\"undefined\"!=typeof Symbol&&e[Symbol.iterator]||e[\"@@iterator\"];if(null!=o){var i=[],a=!0,s=!1;try{for(o=o.call(e);!(a=(r=o.next()).done)&&(i.push(r.value),!t||i.length!==t);a=!0);}catch(e){s=!0,n=e}finally{try{a||null==o.return||o.return()}finally{if(s)throw n}}return i}}n.defineInteropFlag(r),n.export(r,\"_iterable_to_array_limit\",function(){return o}),n.export(r,\"_\",function(){return o})},{\"@parcel/transformer-js/src/esmodule-helpers.js\":\"3ieD1\"}],69769:[function(e,t,r){var n=e(\"@parcel/transformer-js/src/esmodule-helpers.js\");function o(){throw TypeError(\"Invalid attempt to destructure non-iterable instance.\\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method.\")}n.defineInteropFlag(r),n.export(r,\"_non_iterable_rest\",function(){return o}),n.export(r,\"_\",function(){return o})},{\"@parcel/transformer-js/src/esmodule-helpers.js\":\"3ieD1\"}],llvDp:[function(e,t,r){var n=e(\"@parcel/transformer-js/src/esmodule-helpers.js\");n.defineInteropFlag(r),n.export(r,\"_unsupported_iterable_to_array\",function(){return i}),n.export(r,\"_\",function(){return i});var o=e(\"./_array_like_to_array.js\");function i(e,t){if(e){if(\"string\"==typeof e)return(0,o._array_like_to_array)(e,t);var r=Object.prototype.toString.call(e).slice(8,-1);if(\"Object\"===r&&e.constructor&&(r=e.constructor.name),\"Map\"===r||\"Set\"===r)return Array.from(r);if(\"Arguments\"===r||/^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array${'$'}/.test(r))return(0,o._array_like_to_array)(e,t)}}},{\"./_array_like_to_array.js\":\"7l25Y\",\"@parcel/transformer-js/src/esmodule-helpers.js\":\"3ieD1\"}],\"7l25Y\":[function(e,t,r){var n=e(\"@parcel/transformer-js/src/esmodule-helpers.js\");function o(e,t){(null==t||t>e.length)&&(t=e.length);for(var r=0,n=Array(t);r<t;r++)n[r]=e[r];return n}n.defineInteropFlag(r),n.export(r,\"_array_like_to_array\",function(){return o}),n.export(r,\"_\",function(){return o})},{\"@parcel/transformer-js/src/esmodule-helpers.js\":\"3ieD1\"}],\"4Hwej\":[function(e,t,r){var n=e(\"@parcel/transformer-js/src/esmodule-helpers.js\");n.defineInteropFlag(r),n.export(r,\"_to_consumable_array\",function(){return l}),n.export(r,\"_\",function(){return l});var o=e(\"./_array_without_holes.js\"),i=e(\"./_iterable_to_array.js\"),a=e(\"./_non_iterable_spread.js\"),s=e(\"./_unsupported_iterable_to_array.js\");function l(e){return(0,o._array_without_holes)(e)||(0,i._iterable_to_array)(e)||(0,s._unsupported_iterable_to_array)(e)||(0,a._non_iterable_spread)()}},{\"./_array_without_holes.js\":\"l9LPk\",\"./_iterable_to_array.js\":\"5Csja\",\"./_non_iterable_spread.js\":\"4qJ2s\",\"./_unsupported_iterable_to_array.js\":\"llvDp\",\"@parcel/transformer-js/src/esmodule-helpers.js\":\"3ieD1\"}],l9LPk:[function(e,t,r){var n=e(\"@parcel/transformer-js/src/esmodule-helpers.js\");n.defineInteropFlag(r),n.export(r,\"_array_without_holes\",function(){return i}),n.export(r,\"_\",function(){return i});var o=e(\"./_array_like_to_array.js\");function i(e){if(Array.isArray(e))return(0,o._array_like_to_array)(e)}},{\"./_array_like_to_array.js\":\"7l25Y\",\"@parcel/transformer-js/src/esmodule-helpers.js\":\"3ieD1\"}],\"5Csja\":[function(e,t,r){var n=e(\"@parcel/transformer-js/src/esmodule-helpers.js\");function o(e){if(\"undefined\"!=typeof Symbol&&null!=e[Symbol.iterator]||null!=e[\"@@iterator\"])return Array.from(e)}n.defineInteropFlag(r),n.export(r,\"_iterable_to_array\",function(){return o}),n.export(r,\"_\",function(){return o})},{\"@parcel/transformer-js/src/esmodule-helpers.js\":\"3ieD1\"}],\"4qJ2s\":[function(e,t,r){var n=e(\"@parcel/transformer-js/src/esmodule-helpers.js\");function o(){throw TypeError(\"Invalid attempt to spread non-iterable instance.\\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method.\")}n.defineInteropFlag(r),n.export(r,\"_non_iterable_spread\",function(){return o}),n.export(r,\"_\",function(){return o})},{\"@parcel/transformer-js/src/esmodule-helpers.js\":\"3ieD1\"}],hIXl2:[function(e,t,r){var n=e(\"@parcel/transformer-js/src/esmodule-helpers.js\");n.defineInteropFlag(r),n.export(r,\"_\",function(){return o.__generator}),n.export(r,\"_ts_generator\",function(){return o.__generator});var o=e(\"tslib\")},{tslib:\"h1Nlh\",\"@parcel/transformer-js/src/esmodule-helpers.js\":\"3ieD1\"}],h1Nlh:[function(e,t,r){var n=e(\"@parcel/transformer-js/src/esmodule-helpers.js\");n.defineInteropFlag(r),n.export(r,\"__extends\",function(){return a}),n.export(r,\"__assign\",function(){return s}),n.export(r,\"__rest\",function(){return l}),n.export(r,\"__decorate\",function(){return c}),n.export(r,\"__param\",function(){return u}),n.export(r,\"__esDecorate\",function(){return f}),n.export(r,\"__runInitializers\",function(){return p}),n.export(r,\"__propKey\",function(){return d}),n.export(r,\"__setFunctionName\",function(){return y}),n.export(r,\"__metadata\",function(){return _}),n.export(r,\"__awaiter\",function(){return m}),n.export(r,\"__generator\",function(){return h}),n.export(r,\"__createBinding\",function(){return v}),n.export(r,\"__exportStar\",function(){return b}),n.export(r,\"__values\",function(){return g}),n.export(r,\"__read\",function(){return w}),n.export(r,\"__spread\",function(){return j}),n.export(r,\"__spreadArrays\",function(){return x}),n.export(r,\"__spreadArray\",function(){return I}),n.export(r,\"__await\",function(){return E}),n.export(r,\"__asyncGenerator\",function(){return T}),n.export(r,\"__asyncDelegator\",function(){return k}),n.export(r,\"__asyncValues\",function(){return O}),n.export(r,\"__makeTemplateObject\",function(){return S}),n.export(r,\"__importStar\",function(){return D}),n.export(r,\"__importDefault\",function(){return C}),n.export(r,\"__classPrivateFieldGet\",function(){return P}),n.export(r,\"__classPrivateFieldSet\",function(){return B}),n.export(r,\"__classPrivateFieldIn\",function(){return R}),n.export(r,\"__addDisposableResource\",function(){return F}),n.export(r,\"__disposeResources\",function(){return M});var o=e(\"@swc/helpers/_/_type_of\"),i=function(e,t){return(i=Object.setPrototypeOf||({__proto__:[]})instanceof Array&&function(e,t){e.__proto__=t}||function(e,t){for(var r in t)Object.prototype.hasOwnProperty.call(t,r)&&(e[r]=t[r])})(e,t)};function a(e,t){if(\"function\"!=typeof t&&null!==t)throw TypeError(\"Class extends value \"+String(t)+\" is not a constructor or null\");function r(){this.constructor=e}i(e,t),e.prototype=null===t?Object.create(t):(r.prototype=t.prototype,new r)}var s=function(){return(s=Object.assign||function(e){for(var t,r=1,n=arguments.length;r<n;r++)for(var o in t=arguments[r])Object.prototype.hasOwnProperty.call(t,o)&&(e[o]=t[o]);return e}).apply(this,arguments)};function l(e,t){var r={};for(var n in e)Object.prototype.hasOwnProperty.call(e,n)&&0>t.indexOf(n)&&(r[n]=e[n]);if(null!=e&&\"function\"==typeof Object.getOwnPropertySymbols)for(var o=0,n=Object.getOwnPropertySymbols(e);o<n.length;o++)0>t.indexOf(n[o])&&Object.prototype.propertyIsEnumerable.call(e,n[o])&&(r[n[o]]=e[n[o]]);return r}function c(e,t,r,n){var o,i=arguments.length,a=i<3?t:null===n?n=Object.getOwnPropertyDescriptor(t,r):n;if(\"object\"==typeof Reflect&&\"function\"==typeof Reflect.decorate)a=Reflect.decorate(e,t,r,n);else for(var s=e.length-1;s>=0;s--)(o=e[s])&&(a=(i<3?o(a):i>3?o(t,r,a):o(t,r))||a);return i>3&&a&&Object.defineProperty(t,r,a),a}function u(e,t){return function(r,n){t(r,n,e)}}function f(e,t,r,n,o,i){function a(e){if(void 0!==e&&\"function\"!=typeof e)throw TypeError(\"Function expected\");return e}for(var s,l=n.kind,c=\"getter\"===l?\"get\":\"setter\"===l?\"set\":\"value\",u=!t&&e?n.static?e:e.prototype:null,f=t||(u?Object.getOwnPropertyDescriptor(u,n.name):{}),p=!1,d=r.length-1;d>=0;d--){var y={};for(var _ in n)y[_]=\"access\"===_?{}:n[_];for(var _ in n.access)y.access[_]=n.access[_];y.addInitializer=function(e){if(p)throw TypeError(\"Cannot add initializers after decoration has completed\");i.push(a(e||null))};var m=(0,r[d])(\"accessor\"===l?{get:f.get,set:f.set}:f[c],y);if(\"accessor\"===l){if(void 0===m)continue;if(null===m||\"object\"!=typeof m)throw TypeError(\"Object expected\");(s=a(m.get))&&(f.get=s),(s=a(m.set))&&(f.set=s),(s=a(m.init))&&o.unshift(s)}else(s=a(m))&&(\"field\"===l?o.unshift(s):f[c]=s)}u&&Object.defineProperty(u,n.name,f),p=!0}function p(e,t,r){for(var n=arguments.length>2,o=0;o<t.length;o++)r=n?t[o].call(e,r):t[o].call(e);return n?r:void 0}function d(e){return(void 0===e?\"undefined\":(0,o._)(e))===\"symbol\"?e:\"\".concat(e)}function y(e,t,r){return(void 0===t?\"undefined\":(0,o._)(t))===\"symbol\"&&(t=t.description?\"[\".concat(t.description,\"]\"):\"\"),Object.defineProperty(e,\"name\",{configurable:!0,value:r?\"\".concat(r,\" \",t):t})}function _(e,t){if(\"object\"==typeof Reflect&&\"function\"==typeof Reflect.metadata)return Reflect.metadata(e,t)}function m(e,t,r,n){return new(r||(r=Promise))(function(o,i){function a(e){try{l(n.next(e))}catch(e){i(e)}}function s(e){try{l(n.throw(e))}catch(e){i(e)}}function l(e){var t;e.done?o(e.value):((t=e.value)instanceof r?t:new r(function(e){e(t)})).then(a,s)}l((n=n.apply(e,t||[])).next())})}function h(e,t){var r,n,o,i,a={label:0,sent:function(){if(1&o[0])throw o[1];return o[1]},trys:[],ops:[]};return i={next:s(0),throw:s(1),return:s(2)},\"function\"==typeof Symbol&&(i[Symbol.iterator]=function(){return this}),i;function s(s){return function(l){return function(s){if(r)throw TypeError(\"Generator is already executing.\");for(;i&&(i=0,s[0]&&(a=0)),a;)try{if(r=1,n&&(o=2&s[0]?n.return:s[0]?n.throw||((o=n.return)&&o.call(n),0):n.next)&&!(o=o.call(n,s[1])).done)return o;switch(n=0,o&&(s=[2&s[0],o.value]),s[0]){case 0:case 1:o=s;break;case 4:return a.label++,{value:s[1],done:!1};case 5:a.label++,n=s[1],s=[0];continue;case 7:s=a.ops.pop(),a.trys.pop();continue;default:if(!(o=(o=a.trys).length>0&&o[o.length-1])&&(6===s[0]||2===s[0])){a=0;continue}if(3===s[0]&&(!o||s[1]>o[0]&&s[1]<o[3])){a.label=s[1];break}if(6===s[0]&&a.label<o[1]){a.label=o[1],o=s;break}if(o&&a.label<o[2]){a.label=o[2],a.ops.push(s);break}o[2]&&a.ops.pop(),a.trys.pop();continue}s=t.call(e,a)}catch(e){s=[6,e],n=0}finally{r=o=0}if(5&s[0])throw s[1];return{value:s[0]?s[1]:void 0,done:!0}}([s,l])}}}var v=Object.create?function(e,t,r,n){void 0===n&&(n=r);var o=Object.getOwnPropertyDescriptor(t,r);(!o||(\"get\"in o?!t.__esModule:o.writable||o.configurable))&&(o={enumerable:!0,get:function(){return t[r]}}),Object.defineProperty(e,n,o)}:function(e,t,r,n){void 0===n&&(n=r),e[n]=t[r]};function b(e,t){for(var r in e)\"default\"===r||Object.prototype.hasOwnProperty.call(t,r)||v(t,e,r)}function g(e){var t=\"function\"==typeof Symbol&&Symbol.iterator,r=t&&e[t],n=0;if(r)return r.call(e);if(e&&\"number\"==typeof e.length)return{next:function(){return e&&n>=e.length&&(e=void 0),{value:e&&e[n++],done:!e}}};throw TypeError(t?\"Object is not iterable.\":\"Symbol.iterator is not defined.\")}function w(e,t){var r=\"function\"==typeof Symbol&&e[Symbol.iterator];if(!r)return e;var n,o,i=r.call(e),a=[];try{for(;(void 0===t||t-- >0)&&!(n=i.next()).done;)a.push(n.value)}catch(e){o={error:e}}finally{try{n&&!n.done&&(r=i.return)&&r.call(i)}finally{if(o)throw o.error}}return a}function j(){for(var e=[],t=0;t<arguments.length;t++)e=e.concat(w(arguments[t]));return e}function x(){for(var e=0,t=0,r=arguments.length;t<r;t++)e+=arguments[t].length;for(var n=Array(e),o=0,t=0;t<r;t++)for(var i=arguments[t],a=0,s=i.length;a<s;a++,o++)n[o]=i[a];return n}function I(e,t,r){if(r||2==arguments.length)for(var n,o=0,i=t.length;o<i;o++)!n&&o in t||(n||(n=Array.prototype.slice.call(t,0,o)),n[o]=t[o]);return e.concat(n||Array.prototype.slice.call(t))}function E(e){return this instanceof E?(this.v=e,this):new E(e)}function T(e,t,r){if(!Symbol.asyncIterator)throw TypeError(\"Symbol.asyncIterator is not defined.\");var n,o=r.apply(e,t||[]),i=[];return n={},a(\"next\"),a(\"throw\"),a(\"return\"),n[Symbol.asyncIterator]=function(){return this},n;function a(e){o[e]&&(n[e]=function(t){return new Promise(function(r,n){i.push([e,t,r,n])>1||s(e,t)})})}function s(e,t){try{var r;(r=o[e](t)).value instanceof E?Promise.resolve(r.value.v).then(l,c):u(i[0][2],r)}catch(e){u(i[0][3],e)}}function l(e){s(\"next\",e)}function c(e){s(\"throw\",e)}function u(e,t){e(t),i.shift(),i.length&&s(i[0][0],i[0][1])}}function k(e){var t,r;return t={},n(\"next\"),n(\"throw\",function(e){throw e}),n(\"return\"),t[Symbol.iterator]=function(){return this},t;function n(n,o){t[n]=e[n]?function(t){return(r=!r)?{value:E(e[n](t)),done:!1}:o?o(t):t}:o}}function O(e){if(!Symbol.asyncIterator)throw TypeError(\"Symbol.asyncIterator is not defined.\");var t,r=e[Symbol.asyncIterator];return r?r.call(e):(e=g(e),t={},n(\"next\"),n(\"throw\"),n(\"return\"),t[Symbol.asyncIterator]=function(){return this},t);function n(r){t[r]=e[r]&&function(t){return new Promise(function(n,o){!function(e,t,r,n){Promise.resolve(n).then(function(t){e({value:t,done:r})},t)}(n,o,(t=e[r](t)).done,t.value)})}}}function S(e,t){return Object.defineProperty?Object.defineProperty(e,\"raw\",{value:t}):e.raw=t,e}var A=Object.create?function(e,t){Object.defineProperty(e,\"default\",{enumerable:!0,value:t})}:function(e,t){e.default=t};function D(e){if(e&&e.__esModule)return e;var t={};if(null!=e)for(var r in e)\"default\"!==r&&Object.prototype.hasOwnProperty.call(e,r)&&v(t,e,r);return A(t,e),t}function C(e){return e&&e.__esModule?e:{default:e}}function P(e,t,r,n){if(\"a\"===r&&!n)throw TypeError(\"Private accessor was defined without a getter\");if(\"function\"==typeof t?e!==t||!n:!t.has(e))throw TypeError(\"Cannot read private member from an object whose class did not declare it\");return\"m\"===r?n:\"a\"===r?n.call(e):n?n.value:t.get(e)}function B(e,t,r,n,o){if(\"m\"===n)throw TypeError(\"Private method is not writable\");if(\"a\"===n&&!o)throw TypeError(\"Private accessor was defined without a setter\");if(\"function\"==typeof t?e!==t||!o:!t.has(e))throw TypeError(\"Cannot write private member to an object whose class did not declare it\");return\"a\"===n?o.call(e,r):o?o.value=r:t.set(e,r),r}function R(e,t){if(null===t||\"object\"!=typeof t&&\"function\"!=typeof t)throw TypeError(\"Cannot use 'in' operator on non-object\");return\"function\"==typeof e?t===e:e.has(t)}function F(e,t,r){if(null!=t){var n;if(\"object\"!=typeof t&&\"function\"!=typeof t)throw TypeError(\"Object expected.\");if(r){if(!Symbol.asyncDispose)throw TypeError(\"Symbol.asyncDispose is not defined.\");n=t[Symbol.asyncDispose]}if(void 0===n){if(!Symbol.dispose)throw TypeError(\"Symbol.dispose is not defined.\");n=t[Symbol.dispose]}if(\"function\"!=typeof n)throw TypeError(\"Object not disposable.\");e.stack.push({value:t,dispose:n,async:r})}else r&&e.stack.push({async:!0});return t}var N=\"function\"==typeof SuppressedError?SuppressedError:function(e,t,r){var n=Error(r);return n.name=\"SuppressedError\",n.error=e,n.suppressed=t,n};function M(e){function t(t){e.error=e.hasError?new N(t,e.error,\"An error was suppressed during disposal.\"):t,e.hasError=!0}return function r(){for(;e.stack.length;){var n=e.stack.pop();try{var o=n.dispose&&n.dispose.call(n.value);if(n.async)return Promise.resolve(o).then(r,function(e){return t(e),r()})}catch(e){t(e)}}if(e.hasError)throw e.error}()}r.default={__extends:a,__assign:s,__rest:l,__decorate:c,__param:u,__metadata:_,__awaiter:m,__generator:h,__createBinding:v,__exportStar:b,__values:g,__read:w,__spread:j,__spreadArrays:x,__spreadArray:I,__await:E,__asyncGenerator:T,__asyncDelegator:k,__asyncValues:O,__makeTemplateObject:S,__importStar:D,__importDefault:C,__classPrivateFieldGet:P,__classPrivateFieldSet:B,__classPrivateFieldIn:R,__addDisposableResource:F,__disposeResources:M}},{\"@swc/helpers/_/_type_of\":\"cHZd3\",\"@parcel/transformer-js/src/esmodule-helpers.js\":\"3ieD1\"}],cHZd3:[function(e,t,r){var n=e(\"@parcel/transformer-js/src/esmodule-helpers.js\");function o(e){return e&&\"undefined\"!=typeof Symbol&&e.constructor===Symbol?\"symbol\":typeof e}n.defineInteropFlag(r),n.export(r,\"_type_of\",function(){return o}),n.export(r,\"_\",function(){return o})},{\"@parcel/transformer-js/src/esmodule-helpers.js\":\"3ieD1\"}],eKFSM:[function(e,t,r){var n=e(\"@parcel/transformer-js/src/esmodule-helpers.js\");n.defineInteropFlag(r),n.export(r,\"variables\",function(){return l}),n.export(r,\"runSetup\",function(){return c});var o=e(\"@swc/helpers/_/_async_to_generator\"),i=e(\"@swc/helpers/_/_ts_generator\"),a=e(\"./vars\"),s=e(\"./common\"),l=null;function c(e,t,r){return u.apply(this,arguments)}function u(){return(u=(0,o._)(function(e,t,r){return(0,i._)(this,function(n){var o;return l=a.variables,0===(o=document.querySelector(\"body\")).clientHeight&&(o.style.height=\"100%\"),document.addEventListener(\"touchstart\",function(){return!0},!1),(0,s.processVariables)(e,t,r),[2]})})).apply(this,arguments)}},{\"@swc/helpers/_/_async_to_generator\":\"eTGQ5\",\"@swc/helpers/_/_ts_generator\":\"hIXl2\",\"./vars\":\"7Dy8T\",\"./common\":\"h1yCa\",\"@parcel/transformer-js/src/esmodule-helpers.js\":\"3ieD1\"}],\"7Dy8T\":[function(e,t,r){var n=e(\"@parcel/transformer-js/src/esmodule-helpers.js\");n.defineInteropFlag(r),n.export(r,\"variables\",function(){return i});var o=e(\"./types\");o.BackgroundRepeat.NoRepeat,o.BackgroundSize.Cover,o.ButtonActionType.CloseNotification,o.BackgroundRepeat.NoRepeat,o.BackgroundSize.Cover,o.ButtonActionType.CloseNotification,o.ButtonActionType.CloseNotification,o.TextAlign.CENTER,o.TextAlign.CENTER,o.TextAlign.CENTER,o.AspectRatios[\"9_16\"],o.AnimationTypes.MOVE_IN,o.EasingOptions.EASE_OUT,o.ButtonActionType.CloseNotification;var i={\"campaignMeta\": {\"campaignAndBatchId\": \"1736946682_20250129\", \"variantKey\": \"wzrk_default\"}, \"buttons\": [{\"id\": \"button-1\", \"index\": 1, \"name\": \"Button 1\", \"actions\": {\"type\": \"url\", \"text\": \"\", \"android\": \"https://google.com?q=android\", \"ios\": \"https://google.com?q=ios\", \"kv\": {}, \"templateId\": \"\", \"templateName\": \"\", \"vars\": {}}, \"position\": {\"x\": 2, \"y\": 86}, \"size\": {\"width\": 40, \"height\": 7}, \"level\": 13, \"style\": {\"font\": {\"size\": 40, \"color\": \"#FFFFFF\", \"style\": \"normal\", \"weight\": \"normal\"}, \"border\": {\"width\": 0, \"color\": \"#000000\", \"radius\": 0.5}, \"backgroundColor\": \"rgba(106, 228, 203, 1)\", \"activeBackgroundColor\": \"#4A4C4C\", \"boxShadow\": {\"hOffset\": 0, \"vOffset\": 0, \"blur\": 0, \"color\": \"#000000\"}}, \"text\": \"Click me\", \"hidden\": false}, {\"id\": \"button-2\", \"index\": 2, \"name\": \"KV\", \"actions\": {\"type\": \"kv\", \"text\": \"\", \"android\": \"\", \"ios\": \"\", \"kv\": {\"OS\": \"ios\"}, \"templateId\": \"\", \"templateName\": \"\", \"vars\": {}}, \"position\": {\"x\": 58, \"y\": 86}, \"size\": {\"width\": 40, \"height\": 7}, \"level\": 14, \"style\": {\"font\": {\"size\": 40, \"color\": \"#FFFFFF\", \"style\": \"normal\", \"weight\": \"normal\"}, \"border\": {\"width\": 0, \"color\": \"#000000\", \"radius\": 0.5}, \"backgroundColor\": \"#191919\", \"activeBackgroundColor\": \"#4A4C4C\", \"boxShadow\": {\"hOffset\": 0, \"vOffset\": 0, \"blur\": 0, \"color\": \"#000000\"}}, \"text\": \"KV\", \"hidden\": false}], \"overlay\": {\"action\": \"No Action\", \"backgroundColor\": \"rgba(0, 0, 0, 0)\", \"backgroundImage\": null, \"backgroundImageName\": null, \"backgroundRepeat\": \"no-repeat\", \"backgroundSize\": \"cover\", \"dismissOnTapOrClick\": true}, \"images\": [{\"id\": \"image-1\", \"index\": 1, \"name\": \"Image 1\", \"position\": {\"x\": 0, \"y\": 0}, \"size\": {\"width\": 100, \"height\": 38}, \"actions\": {\"type\": \"\", \"text\": \"\", \"android\": \"\", \"ios\": \"\", \"kv\": {}, \"templateId\": \"\", \"templateName\": \"\", \"vars\": {}}, \"image\": \"https://dsl810bactgru.cloudfront.net/1696322176/assets/f4532fed0a044d2ab62466d890811f75.jpeg\", \"imageName\": \"4E689C68-F530-4CDE-8D09-75F9EDA4D5AB(1).jpeg\", \"level\": 11}], \"textElements\": [{\"id\": \"text-1\", \"index\": 1, \"name\": \"Text 1\", \"text\": {\"text\": \"some text\\nwith new lines\\njust in case\"}, \"position\": {\"x\": 16, \"y\": 53}, \"size\": {\"width\": 70, \"height\": 19}, \"level\": 12, \"style\": {\"font\": {\"size\": 40, \"color\": \"#000000\", \"style\": \"normal\", \"weight\": \"normal\"}, \"border\": {\"width\": 0, \"color\": \"#FFFFFF\", \"radius\": 0.5}, \"backgroundColor\": \"rgba(255, 255, 255, 0)\"}, \"textAlign\": \"center\"}], \"content\": {\"backgroundColor\": \"#FFFFFF\", \"backgroundImage\": null, \"backgroundImageName\": null, \"backgroundImageSize\": null, \"margin\": 10, \"height\": 60, \"width\": 80, \"aspectRatio\": \"9 / 16\", \"borderRadius\": 2, \"animation\": {\"type\": \"instant\", \"duration\": 1000, \"easing\": null, \"bezier\": null, \"moveInDirection\": null}, \"font\": {\"fontName\": \"Arial\", \"fontFamily\": \"sans-serif\", \"fallbackFonts\": [\"sans-serif\"], \"fontUrl\": \"\"}}, \"dismissButtonProps\": {\"id\": \"dismissButton\", \"index\": 0, \"name\": \"Dismiss Button\", \"actions\": {\"type\": \"close\", \"text\": \"\", \"android\": \"\", \"ios\": \"\", \"kv\": {}, \"templateId\": \"\", \"templateName\": \"\", \"vars\": {}}, \"position\": {\"x\": 89, \"y\": 1}, \"size\": {\"width\": 10, \"height\": 5.625}, \"level\": 100, \"style\": {\"border\": {\"width\": 0, \"color\": \"#000000\", \"radius\": 10}, \"backgroundColor\": \"#191919\", \"activeBackgroundColor\": \"#191919\", \"boxShadow\": {\"hOffset\": 0, \"vOffset\": 0, \"blur\": 0, \"color\": \"#000000\"}, \"icon\": {\"strokeColor\": \"white\", \"strokeWidth\": 7}}, \"text\": \"Click me\", \"hidden\": false}}},{\"./types\":\"eA9ym\",\"@parcel/transformer-js/src/esmodule-helpers.js\":\"3ieD1\"}],eA9ym:[function(e,t,r){var n,o,i,a,s,l,c,u,f,p,d,y,_,m,h,v,b,g,w,j,x=e(\"@parcel/transformer-js/src/esmodule-helpers.js\");x.defineInteropFlag(r),x.export(r,\"AnimationTypes\",function(){return g}),x.export(r,\"MoveInDirections\",function(){return w}),x.export(r,\"EasingOptions\",function(){return j}),x.export(r,\"AspectRatios\",function(){return v}),x.export(r,\"BackgroundRepeat\",function(){return m}),x.export(r,\"BackgroundSize\",function(){return h}),x.export(r,\"ButtonActionType\",function(){return _}),x.export(r,\"Platform\",function(){return y}),x.export(r,\"TextAlign\",function(){return b}),x.export(r,\"VariableTypes\",function(){return d}),(n=d||(d={})).overlay=\"overlay\",n.content=\"content\",n.buttons=\"buttons\",n.images=\"images\",n.textElements=\"textElements\",n.dismissButtonProps=\"dismissButtonProps\",(o=y||(y={})).ANDROID=\"android\",o.IOS=\"ios\",(i=_||(_={})).None=\"\",i.CloseNotification=\"close\",i.OpenUrl=\"url\",i.CustomKeyValuePairs=\"kv\",i.CustomFunction=\"custom-code\",(a=m||(m={})).NoRepeat=\"no-repeat\",a.Repeat=\"repeat\",a.RepeatX=\"repeat-x\",a.RepeatY=\"repeat-y\",(s=h||(h={})).Auto=\"auto\",s.Contain=\"contain\",s.Cover=\"cover\",(l=v||(v={}))[\"16_9\"]=\"16 / 9\",l[\"3_4\"]=\"3 / 4\",l[\"9_16\"]=\"9 / 16\",l[\"1_1\"]=\"1 / 1\",(c=b||(b={})).LEFT=\"left\",c.CENTER=\"center\",c.RIGHT=\"right\",(u=g||(g={})).INSTANT=\"instant\",u.DISSOLVE=\"dissolve\",u.MOVE_IN=\"move-in\",(f=w||(w={})).LEFT=\"left\",f.RIGHT=\"right\",f.TOP=\"top\",f.BOTTOM=\"bottom\",(p=j||(j={})).LINEAR=\"linear\",p.EASE_IN=\"ease-in\",p.EASE_OUT=\"ease-out\",p.EASE_IN_AND_OUT=\"ease-in-out\",p.CUBIC_BEZIER=\"cubic-bezier\"},{\"@parcel/transformer-js/src/esmodule-helpers.js\":\"3ieD1\"}],h1yCa:[function(e,t,r){var n=e(\"@parcel/transformer-js/src/esmodule-helpers.js\");n.defineInteropFlag(r),n.export(r,\"processVariables\",function(){return u}),n.export(r,\"getDomElementById\",function(){return p}),n.export(r,\"runTrackedAction\",function(){return d}),n.export(r,\"preloadImage\",function(){return h}),n.export(r,\"loadCustomFont\",function(){return b});var o=e(\"@swc/helpers/_/_async_to_generator\"),i=e(\"@swc/helpers/_/_sliced_to_array\"),a=e(\"@swc/helpers/_/_ts_generator\"),s=e(\"./setup\"),l=e(\"./types\"),c={};function u(e,t,r){return f.apply(this,arguments)}function f(){return(f=(0,o._)(function(e,t,r){var n,o,l,c,u,f,p,d;return(0,a._)(this,function(a){switch(a.label){case 0:return[4,e(s.variables)];case 1:a.sent(),n=!0,o=!1,l=void 0;try{for(c=Object.entries(s.variables)[Symbol.iterator]();!(n=(u=c.next()).done);n=!0)if(p=(f=(0,i._)(u.value,2))[0],d=f[1],\"function\"==typeof r&&r({variable:p,value:d}))continue}catch(e){o=!0,l=e}finally{try{n||null==c.return||c.return()}finally{if(o)throw l}}return\"function\"==typeof t&&t(s.variables),[2]}})})).apply(this,arguments)}function p(e){return function(e){var t=c[e];if(t)return t;var r=document.querySelector(e);return r||(\"devMode\"in s.variables&&s.variables.devMode&&console.log(\"No selector found for \".concat(e)),r={style:{}}),r}(e)}function d(e){var t=e.actions;switch(t.type){case l.ButtonActionType.None:break;case l.ButtonActionType.OpenUrl:case l.ButtonActionType.CloseNotification:!function(e,t,r){if(_()){y(e,t,r);return}var n=\"\";n=(n=e.type===l.ButtonActionType.CloseNotification?\"wzrk://thisleadstonowhere\":m()===l.Platform.ANDROID?e.android:e.ios).replace(/\\\\\"/g,'\"'),n+=(n.includes(\"?\")?\"&\":\"?\")+\"wzrk_c2a=\".concat(t,\"&button_id=\").concat(r);var o=document.createElement(\"a\");o.setAttribute(\"href\",n);var i=document.createEvent(\"MouseEvents\");i.initEvent(\"click\",!0,!1),o.dispatchEvent(i)}(t,e.name,e.id);break;default:_()?y(t,e.name,e.id):console.log(\"Unable to run app action as it is not supported by the sdk\",e)}}function y(e,t,r){var n=m();n===l.Platform.IOS?window.webkit.messageHandlers.clevertap.postMessage({action:\"triggerInAppAction\",actionJson:e,callToAction:t,buttonId:r}):n===l.Platform.ANDROID&&window.CleverTap.triggerInAppAction(JSON.stringify(e),t,r)}function _(){var e=m();return e===l.Platform.IOS?!!(window.cleverTapIOSSDKVersion&&window.cleverTapIOSSDKVersion>=7e4):e===l.Platform.ANDROID&&window.CleverTap.getSdkVersion&&window.CleverTap.getSdkVersion()>=7e4}function m(){var e,t;return(null===(e=window.webkit)||void 0===e?void 0:null===(t=e.messageHandlers)||void 0===t?void 0:t.clevertap)?l.Platform.IOS:window.CleverTap?l.Platform.ANDROID:\"devMode\"in s.variables&&s.variables.devMode?null:void console.error(\"can not communicate to sdk\")}function h(e){return new Promise(function(t,r){var n=new Image;n.onload=t,n.onerror=r,n.src=e})}var v=\"customFontElement\";function b(e){var t=document.getElementById(v);t&&t.parentNode.removeChild(t);var r=document.createElement(\"link\");return r.id=v,r.rel=\"stylesheet\",document.head.appendChild(r),new Promise(function(t,n){r.onload=t,r.onerror=n,r.href=e})}},{\"@swc/helpers/_/_async_to_generator\":\"eTGQ5\",\"@swc/helpers/_/_sliced_to_array\":\"jzEiA\",\"@swc/helpers/_/_ts_generator\":\"hIXl2\",\"./setup\":\"eKFSM\",\"./types\":\"eA9ym\",\"@parcel/transformer-js/src/esmodule-helpers.js\":\"3ieD1\"}]},[\"h9QmU\"],\"h9QmU\",\"parcelRequire49af\");</script> </body></html>"
      }
    }
    """.trimIndent()
}