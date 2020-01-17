package memphis.myapplication;

import net.named_data.jndn.Name;

import org.junit.Test;

import memphis.myapplication.data.Common;

import static org.junit.Assert.assertEquals;



public class EditComponentTest {
    @Test
    public void middleComponent() {
        Name name = new Name("/test0/test1/test2/test3");
        Name newName = new Name("/test0/test1/test-1/test3");
        assertEquals("setComponent", newName, Common.setComponent(name, 2, "test-1"));
    }

    @Test
    public void firstComponent() {
        Name name = new Name("/test0/test1/test2/test3");
        Name newName = new Name("/test-1/test1/test2/test3");
        assertEquals("setComponent", newName, Common.setComponent(name, 0, "test-1"));
    }

    @Test
    public void finalComponent() {
        Name name = new Name("/test0/test1/test2/test3");
        Name newName = new Name("/test0/test1/test2/test-1");
        assertEquals("setComponent", newName, Common.setComponent(name, 3, "test-1"));
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void indexTooHigh() {
        Name name = new Name("/test0/test1/test2/test3");
        Common.setComponent(name, 7, "test-1");
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void indexTooLow() {
        Name name = new Name("/test0/test1/test2/test3");
        Common.setComponent(name, -1, "test-1");
    }
}
