import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;

public class OnTheFlyCompilerTest {

    @Test
    public void class_with_instance_variables_can_be_created() throws CannotCompileException, NoSuchFieldException, IllegalAccessException, InstantiationException {
        ClassPool pool = ClassPool.getDefault();
        CtClass aClassCt = pool.makeClass("AClass");
        aClassCt.addField(CtField.make("public String aString = \"stringValue\";", aClassCt));
        aClassCt.addField(CtField.make("public int anInteger = 434343;", aClassCt));
        aClassCt.addField(CtField.make("public char aChar = 'l';", aClassCt));
        aClassCt.addField(CtField.make("public boolean aBoolean = true;", aClassCt));

        Class aClass = aClassCt.toClass();
        Object anObjectOfAClass = aClass.newInstance();

        Assert.assertThat(aClass.getField("aString").get(anObjectOfAClass), is("stringValue"));
        Assert.assertThat(aClass.getField("anInteger").get(anObjectOfAClass), is(434343));
        Assert.assertThat(aClass.getField("aChar").get(anObjectOfAClass), is('l'));
        Assert.assertThat(aClass.getField("aBoolean").get(anObjectOfAClass), is(true));
    }

}
