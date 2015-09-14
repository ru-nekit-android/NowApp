package ru.nekit.android.nowapp;

/**
 * Created by MacOS on 16.07.15.
 */

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class NowAppTest {

    @Before
    public void setUp() throws Exception {
        // setup
    }

    @Test
    public void testSomething() throws Exception {
        Assert.assertEquals(true , 1==1);
    }

    @Test
    public void testSomethin2g() throws Exception {
        Assert.assertEquals(true , 1==1);
    }
}