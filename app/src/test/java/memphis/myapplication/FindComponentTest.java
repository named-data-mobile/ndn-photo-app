package memphis.myapplication;

import net.named_data.jndn.Name;

import org.junit.Test;

import memphis.myapplication.data.Common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FindComponentTest {
    @Test
    public void returnsTrue() {
        Name name = new Name("/test0/test1/test2/test3");
        String component = "test2";
        assertEquals(2, Common.discoverComponent(name, component));
    }

    @Test
    public void returnsFalse() {
        Name name = new Name("/test0/test1/test2/test3");
        String component = "test-1";
        assertTrue(-1 == Common.discoverComponent(name, component));
    }

}
