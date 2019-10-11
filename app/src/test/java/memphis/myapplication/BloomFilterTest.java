package memphis.myapplication;

import net.named_data.jndn.Name;

import org.junit.Test;

import memphis.myapplication.utilities.BloomFilter;

import static org.junit.Assert.assertTrue;

public class BloomFilterTest {
    @Test
    public void recreateTest() {
        BloomFilter bloomFilter = new BloomFilter(10,0.001);
        for (int i = 0; i<10; i++) {
            bloomFilter.insert("test"+i);
        }

        Name bloomName = new Name(bloomFilter.appendToName(new Name()));

        try {
            BloomFilter newBloomFilter = new BloomFilter(bloomName.get(0).toNumber(), 0.001, bloomName.get(-1));
            boolean check = true;
            for (int i = 0; i<10; i++) {
                check = newBloomFilter.contains("test"+i);
            }
            assertTrue("recreateBloomFilter", check);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
