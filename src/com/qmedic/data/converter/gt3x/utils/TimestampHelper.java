/******************************************************************************************
 * 
 * Copyright (c) 2016 EveryFit, Inc.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * Authors:
 *  - Albinali, Fahd
 *  - Billy, Stanis Laus
 * 
 ******************************************************************************************/

package com.qmedic.data.converter.gt3x.utils;

public class TimestampHelper {
	private int _denominator;
    private  int _numerator;
    private  int _quotient;
    private int _count;

    public TimestampHelper(int dividend, int divisor)
    {
        int quotient = dividend/divisor;
        int remainder = dividend%divisor;
        int gcd = GreatestCommonDivisor(divisor, remainder);

        _quotient = quotient;
        _numerator = remainder/gcd;
        _denominator = divisor/gcd;
        _count = 0;

        Reset();
    }

    private int GreatestCommonDivisor(int a, int b)
    {
        while (0 != a)
        {
            int c = a;
            a = b%a;
            b = c;
        }

        return b;
    }

    public int Next()
    {
        int tick = _quotient + (_count < _numerator ? 1 : 0);

        if (_denominator == ++_count)
            Reset();

        return tick;
    }

    public void Reset()
    {
        _count = 0;
    }
}
