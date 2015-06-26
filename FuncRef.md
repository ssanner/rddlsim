# Introduction #

This page serves as a reference for the built-in functions available in RDDL. The documentation here is based on the implementation of rddl.RDDL.java.


# Math Functions #

  * div `[` x, y `]` : divides y by x. Arguments x and y must be integer.
  * mod `[` x, y `]` : returns x % y. Arguments x and y must be integer.
  * min `[` x, y `]` : minimum of x and y
  * max `[` x, y `]` : maximum of x and y

  * abs `[` x `]` : the absolute value of x
  * sgn `[` x `]` : returns 0 if x == 0; 1 if x > 0; -1 if x < 0
  * round `[` x `]` : rounds x to the nearest integer
  * floor `[` x `]` : returns the greatest integer less than x
  * ceil `[` x `]` : returns the smallest integer greater than x

## Exponential and Logarithmic Functions ##

  * log `[` x, b `]` : returns the logarithm of x with base b
  * ln `[` x `]` : returns the natural logarithm with base equal to Euler's constant
  * exp `[` x `]` : returns e^x where e is Euler's constant
  * pow `[` b, x `]` : returns b^x
  * sqrt `[` x `]` : returns the square root of x

## Trigonometric Functions ##

In this section the argument theta represents an angle in radians.

  * cos `[` theta `]` : returns the cosine of theta
  * sin `[` theta `]` : returns the sine of theta
  * tan `[` theta `]` : returns the tangent of theta
  * acos `[` x `]` : returns the arc cosine of x
  * asin `[` x `]` : returns the arc sine of x
  * atan `[` x `]` : returns the arc tangent of x
  * cosh `[` x `]` : returns the hyperbolic cosine of x
  * sinh `[` x `]` : returns the hyperbolic sine of x
  * tanh `[` x `]` : returns the hyperbolic tangent of x