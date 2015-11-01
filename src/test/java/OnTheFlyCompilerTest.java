import javassist.*;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;

import static org.hamcrest.CoreMatchers.is;

public class OnTheFlyCompilerTest {

    @Test
    public void class_with_instance_variables_can_be_created() throws CannotCompileException, NoSuchFieldException, IllegalAccessException, InstantiationException, NotFoundException {
        ClassPool pool = ClassPool.getDefault();
        CtClass aClassCt = pool.makeClass("AClass");
        aClassCt.addField(CtField.make("public String aString = \"stringValue\";", aClassCt));
        aClassCt.addField(CtField.make("public int anInteger = 434343;", aClassCt));
        String bigIntegerFieldSource = "public java.math.BigInteger aBigInteger = new java.math.BigInteger(\"5844\");";
        aClassCt.addField(CtField.make(bigIntegerFieldSource, aClassCt));
        aClassCt.addField(CtField.make("public char aChar = 'l';", aClassCt));
        aClassCt.addField(CtField.make("public boolean aBoolean = true;", aClassCt));

        Class<?> aClass = aClassCt.toClass();
        Object anObjectOfAClass = aClass.newInstance();

        Assert.assertThat(aClass.getField("aString").get(anObjectOfAClass), is("stringValue"));
        Assert.assertThat(aClass.getField("anInteger").get(anObjectOfAClass), is(434343));
        Assert.assertThat(aClass.getField("aBigInteger").get(anObjectOfAClass), is(new BigInteger("5844")));
        Assert.assertThat(aClass.getField("aChar").get(anObjectOfAClass), is('l'));
        Assert.assertThat(aClass.getField("aBoolean").get(anObjectOfAClass), is(true));
    }

    @Test
    public void class_with_methods_can_be_created()
            throws CannotCompileException, IllegalAccessException,
            InstantiationException, NoSuchMethodException, InvocationTargetException {
        ClassPool pool = ClassPool.getDefault();
        CtClass aClassCt = pool.makeClass("AnotherClass");

        aClassCt.addMethod(CtMethod.make("public String echo(String msg) { return msg; }", aClassCt));

        Class<?> aClass = aClassCt.toClass();
        Object anObjectOfAClass = aClass.newInstance();

        Assert.assertThat(aClass.getMethod("echo", String.class).invoke(anObjectOfAClass, "A message"),
                is("A message"));
    }

}
