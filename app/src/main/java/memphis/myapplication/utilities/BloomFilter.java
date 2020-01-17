package memphis.myapplication.utilities;

/* This file incorporates work covered by the following copyright and
 * permission notice:
 * The MIT License (MIT)
 * Copyright (c) 2000 Arash Partow
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import net.named_data.jndn.Name;
import net.named_data.jndn.Name.Component;

import java.math.BigInteger;
import java.util.Random;

import timber.log.Timber;

public class BloomFilter {
    private static int bits_per_char = 0x08;
    private static long bit_mask[] = new long[] {
            0x01,  //00000001
            0x02,  //00000010
            0x04,  //00000100
            0x08,  //00001000
            0x10,  //00010000
            0x20,  //00100000
            0x40,  //01000000
            0x80   //10000000
    };
    private static double default_false_probability_probability = 0.001;

    long[] salt_;
    byte[]             bit_table_;
    long            salt_count_ = 0;
    long            table_size_ = 0; // 8 * raw_table_size;
    long            raw_table_size_ = 0;
    long            projected_element_count_ = 0;
    long            inserted_element_count_ = 0;
    BigInteger      random_seed_ = BigInteger.ZERO;
    double                  desired_false_positive_probability_ = 0.0;


    public void
    generate_unique_salt()
    {
	    /*
	      Note:
	      A distinct hash function need not be implementation-wise
	      distinct. In the current implementation "seeding" a common
	      hash function with different values seems to be adequate.
	    */
        long predef_salt_count = 128;
        long[]  predef_salt = new long[(int) predef_salt_count];

        long [] tmpSalt = new long[] { 0xAAAAAAAA, 0x55555555, 0x33333333, 0xCCCCCCCC,
                0x66666666, 0x99999999, 0xB5B5B5B5, 0x4B4B4B4B,
                0xAA55AA55, 0x55335533, 0x33CC33CC, 0xCC66CC66,
                0x66996699, 0x99B599B5, 0xB54BB54B, 0x4BAA4BAA,
                0xAA33AA33, 0x55CC55CC, 0x33663366, 0xCC99CC99,
                0x66B566B5, 0x994B994B, 0xB5AAB5AA, 0xAAAAAA33,
                0x555555CC, 0x33333366, 0xCCCCCC99, 0x666666B5,
                0x9999994B, 0xB5B5B5AA, 0xFFFFFFFF, 0xFFFF0000,
                0xB823D5EB, 0xC1191CDF, 0xF623AEB3, 0xDB58499F,
                0xC8D42E70, 0xB173F616, 0xA91A5967, 0xDA427D63,
                0xB1E8A2EA, 0xF6C0D155, 0x4909FEA3, 0xA68CC6A7,
                0xC395E782, 0xA26057EB, 0x0CD5DA28, 0x467C5492,
                0xF15E6982, 0x61C6FAD3, 0x9615E352, 0x6E9E355A,
                0x689B563E, 0x0C9831A8, 0x6753C18B, 0xA622689B,
                0x8CA63C47, 0x42CC2884, 0x8E89919B, 0x6EDBD7D3,
                0x15B6796C, 0x1D6FDFE4, 0x63FF9092, 0xE7401432,
                0xEFFE9412, 0xAEAEDF79, 0x9F245A31, 0x83C136FC,
                0xC3DA4A8C, 0xA5112C8C, 0x5271F491, 0x9A948DAB,
                0xCEE59A8D, 0xB5F525AB, 0x59D13217, 0x24E7C331,
                0x697C2103, 0x84B0A460, 0x86156DA9, 0xAEF2AC68,
                0x23243DA5, 0x3F649643, 0x5FA495A8, 0x67710DF8,
                0x9A6C499E, 0xDCFB0227, 0x46A43433, 0x1832B07A,
                0xC46AFF3C, 0xB9C8FFF0, 0xC9500467, 0x34431BDF,
                0xB652432B, 0xE367F12B, 0x427F4C1B, 0x224C006E,
                0x2E7E5A89, 0x96F99AA5, 0x0BEB452A, 0x2FD87C39,
                0x74B2E1FB, 0x222EFD24, 0xF357F60C, 0x440FCB1E,
                0x8BBE030F, 0x6704DC29, 0x1144D12F, 0x948B1355,
                0x6D8FD7E9, 0x1C11A014, 0xADD1592F, 0xFB3C712E,
                0xFC77642F, 0xF9C4CE8C, 0x31312FB9, 0x08B0DD79,
                0x318FA6E7, 0xC040D23D, 0xC0589AA7, 0x0CA5C075,
                0xF874B172, 0x0CF914D5, 0x784D3280, 0x4E8CFEBC,
                0xC569F575, 0xCDB2A091, 0x2CC016B4, 0x5C5F4421
        };

        int j = 0;
        for (long seed : tmpSalt) {
            predef_salt[j++] = seed;
        }

        if (salt_count_ <= predef_salt_count)
        {
            long[] temp_salt = new long[(int) salt_count_];
            for (int i = 0; i < predef_salt.length; i++) {
                if (i < salt_count_) {
                    temp_salt[i] = (predef_salt[i]);
                }
            }
            salt_ = temp_salt;

            for (int i = 0; i < salt_.length; ++i)
            {
                BigInteger t2 = random_seed_.and(new BigInteger("FFFFFFFF", 16));
                salt_[i] = (salt_[i] * salt_[(i + 3) % salt_.length]) + t2.longValue();
            }
        }
        else
        {
            long[] temp_salt = new long[(int) salt_count_];
            for (int i = 0; i < predef_salt.length; i++) {
                if (i < salt_count_) {
                    temp_salt[i] = (predef_salt[i]);
                }
            }
            salt_ = temp_salt;

            Random generator = new Random(random_seed_.intValue());

            while (salt_.length < salt_count_)
            {
                long current_salt = generator.nextLong() * generator.nextLong();
                if (0 == current_salt) {
                    continue;
                }
                boolean contains = false;
                for (int i = 0; i < salt_.length; i++) {
                    if (salt_[i] == current_salt) {
                        contains = true;
                    }
                }
                if (!contains)
                {
                    int oldLength = salt_.length;
                    salt_ = new long[oldLength + 1];
                    salt_[oldLength] = current_salt;
                }
            }
        }
    }

    public
    BloomFilter(BloomParameters p) {
        projected_element_count_ = p.projected_element_count;
        BigInteger tmp = new BigInteger("A5A5A5A5", 16);
        BigInteger tmp2 = p.random_seed.multiply(tmp);
        random_seed_ = tmp2.add(BigInteger.ONE).and(new BigInteger("FFFFFFFFFFFFFFFF", 16));
        desired_false_positive_probability_ = p.false_positive_probability;
        salt_count_ = p.optimal_parameters.number_of_hashes;
        table_size_ = p.optimal_parameters.table_size;
        generate_unique_salt();
        raw_table_size_ = table_size_ / bits_per_char;
        bit_table_ = new byte[(int) raw_table_size_];
    }

    public
    BloomFilter(long projected_element_count,
                double false_positive_probability) {
        this(getParameters(projected_element_count, false_positive_probability));
    }

    public
    BloomFilter(long projected_element_count) {
        this(getParameters(projected_element_count, default_false_probability_probability));
    }

    public
    BloomFilter(long projected_element_count,
                Component bfName) throws Exception {
        this(projected_element_count, default_false_probability_probability);

        byte[] table = bfName.getValue().getImmutableArray();
        if (table.length != raw_table_size_) {
            throw new Exception("Received BloomFilter cannot be decoded!");
        }
        bit_table_ = table;
    }

    public
    BloomFilter(long projected_element_count,
                double false_positive_probability,
                Component bfName) throws Exception {
        this(projected_element_count, false_positive_probability);

        byte[] table = bfName.getValue().getImmutableArray();
        if (table.length != raw_table_size_) {
            throw new Exception("Received BloomFilter cannot be decoded!");
        }
        bit_table_ = table;
    }

    static BloomParameters
    getParameters(long projected_element_count,
                  double false_positive_probability)
    {
        BloomParameters opt = new BloomParameters();
        opt.false_positive_probability = false_positive_probability;
        opt.projected_element_count = projected_element_count;

        if (opt.not()) {
           Timber.d("Bloom parameters are not correct!");
        }

        opt.compute_optimal_parameters();
        return opt;
    }

    public void
    compute_indices(long hash, long bit_index, long bit)
    {
        bit_index = hash % table_size_;
        bit = bit_index % bits_per_char;
    }

    public void
    insert(String key)
    {
        long bit_index = 0;
        long bit = 0;
        for (int i = 0; i < salt_.length; ++i)
        {
            long hash = murmurHash3((int) salt_[i], key);
            bit_index = hash % table_size_;
            bit = bit_index % bits_per_char;
            long value = bit_table_[(int) (bit_index/bits_per_char)] | bit_mask[(int) bit];
            bit_table_[(int) (bit_index/bits_per_char)] = (byte) value;
        }
        ++inserted_element_count_;
    }

    public boolean
    contains(String key)
    {
        long bit_index = 0;
        long bit = 0;

        for (int i = 0; i < salt_.length; ++i)
        {
            long hash = murmurHash3((int) salt_[i], key);
            bit_index = hash % table_size_;
            bit = bit_index % bits_per_char;
            byte bitTableEntry = (byte) (bit_table_[(int) (bit_index/bits_per_char)] & bit_mask[(int) bit]);
            if (toUnsignedInt(bitTableEntry) != bit_mask[(int) bit]) {
                return false;
            }
        }

        return true;
    }

    public String toString() {
        String out = "";
        int i = 0;
        for (byte element : bit_table_) {
            out += element;
        }
        return out;
    }

    public Name
    appendToName(Name prefix) {
        prefix.append(Component.fromNumber(projected_element_count_));
        prefix.append(Component.fromNumber((int)(desired_false_positive_probability_ * 1000)));
        prefix.append(bit_table_);
        return prefix;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof BloomFilter)) {
            return false;
        }

        byte[] otherTable = ((BloomFilter) o).bit_table_;
        if (otherTable.length != bit_table_.length) {
            return false;
        }

        for (int i = 0; i < bit_table_.length; i++) {
            if (otherTable[i] != bit_table_[i]) {
                return false;
            }
        }
        return true;
    }

    public static long murmurHash3(int seed, long key)
    {
        com.google.common.hash.HashFunction hashImplementation = Hashing.murmur3_32(seed);
        return toUnsignedLong(hashImplementation.newHasher().putLong(key).hash().asInt());
    }

    public static long murmurHash3(int seed, int key)
    {
        com.google.common.hash.HashFunction hashImplementation = Hashing.murmur3_32(seed);
        return toUnsignedLong(hashImplementation.newHasher().putInt(key).hash().asInt());
    }

    public static long murmurHash3(int seed, String key)
    {
        com.google.common.hash.HashFunction hashImplementation = Hashing.murmur3_32(seed);
        return toUnsignedLong(hashImplementation.newHasher().putString(key, Charsets.UTF_8).hash().asInt());
    }

    public static long toUnsignedLong(int x) {
        return x & 0x00000000ffffffffL;
    }

    public static int toUnsignedInt(byte x) { return x & 0xFF; }

}
class BloomParameters
{

    private static int bits_per_char = 0x08;
    public class optimal_parameters_t {
        public long number_of_hashes = 0;
        public long table_size = 0;

    }
    public long						minimum_size = 1;
    public long						maximum_size = Long.MAX_VALUE;
    public long						minimum_number_of_hashes = 1;
    public long		         	  	maximum_number_of_hashes = Long.MAX_VALUE;
    public long						projected_element_count = 200;
    public double	                false_positive_probability = 1 / projected_element_count;
    public BigInteger				random_seed = new BigInteger("A5A5A5A55A5A5A5A", 16);

    public optimal_parameters_t   	optimal_parameters = new optimal_parameters_t();

    public boolean
    compute_optimal_parameters() {
        if (not()) {
            return false;
        }

        double min_m = Double.MAX_VALUE;
        double min_k = 0.0;
        double curr_m = 0.0;
        double k = 1.0;

        while (k < 1000.0)
        {
            double numerator   = (- k * projected_element_count);
            double denominator = Math.log(1.0 - Math.pow(false_positive_probability, (1.0 / k)));
            curr_m = numerator / denominator;
            if (curr_m < min_m)
            {
                min_m = curr_m;
                min_k = k;
            }
            k += 1.0;
        }

        optimal_parameters_t optp = optimal_parameters;

        optp.number_of_hashes = (long) min_k;
        optp.table_size = (long) min_m;
        optp.table_size += (((optp.table_size % bits_per_char) != 0) ? (bits_per_char - (optp.table_size % bits_per_char)) : 0);

        if (optp.number_of_hashes < minimum_number_of_hashes)
            optp.number_of_hashes = minimum_number_of_hashes;
        else if (optp.number_of_hashes > maximum_number_of_hashes)
            optp.number_of_hashes = maximum_number_of_hashes;

        if (optp.table_size < minimum_size)
            optp.table_size = minimum_size;
        else if (optp.table_size > maximum_size)
            optp.table_size = maximum_size;

        return true;
    }
    public boolean not()
    {
        return (minimum_size > maximum_size)      ||
                (minimum_number_of_hashes > maximum_number_of_hashes) ||
                (minimum_number_of_hashes < 1)     ||
                (0 == maximum_number_of_hashes)    ||
                (0 == projected_element_count)     ||
                (false_positive_probability < 0.0) ||
                (random_seed == BigInteger.ZERO);
    }

}

