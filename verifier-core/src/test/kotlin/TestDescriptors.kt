import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import org.junit.Assert
import org.junit.Test

class TestDescriptors {

  @Test
  fun field() {
    val fieldFrom = FieldReference("org/some/Class", "someField", "Ljava/lang/Object;")
    Assert.assertEquals("org.some.Class.someField : Object", fieldFrom.toString())
  }

  @Test
  fun method() {
    val methodFrom = MethodReference("org/some/Class", "someMethod", "(Ljava/lang/String;IF)Ljava/lang/Comparable;")
    Assert.assertEquals("org.some.Class.someMethod(String, int, float) : Comparable", methodFrom.toString())
  }

  @Test
  fun `class`() {
    val classFrom = ClassReference("org/some/Class")
    Assert.assertEquals("org.some.Class", classFrom.toString())
  }
}