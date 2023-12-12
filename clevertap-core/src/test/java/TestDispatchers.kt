import com.clevertap.android.sdk.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler

class TestDispatchers(
    private val testCoroutineScheduler: TestCoroutineScheduler? = null
) : DispatcherProvider {

    override fun io(): CoroutineDispatcher = StandardTestDispatcher(scheduler = testCoroutineScheduler)

    override fun main(): CoroutineDispatcher = StandardTestDispatcher(scheduler = testCoroutineScheduler)

    override fun processing(): CoroutineDispatcher = StandardTestDispatcher(scheduler = testCoroutineScheduler)
}