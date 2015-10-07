# Copyright 2014, Gurobi Optimization, Inc.
#
# This example formulates and solves the following simple QCP model:
#  minimize
#        x^2 + x*y + y^2 + y*z + z^2 + x + 2 y + 3 z
#  subject to
#        x + 2 y + 3z    >= 4
#        x +   y         >= 1
#                      t  = sqrt(2)/2
#        [ x ^ 2 + y ^ 2 - t ^ 2 ] < = 0  (a second-order cone constraint)

library("gurobi")

model <- list()

model$A      <- matrix(c(1,2,3,0,1,1,0,0,0,0,0,1), nrow=3, byrow=T)
model$Q      <- matrix(c(2,1,0,0,1,2,1,0,0,1,2,0,0,0,0,0), nrow=4, byrow=T)
model$cones  <- list(list(4,1,2))
model$obj    <- c(1,2,3,0)
model$rhs    <- c(4,1,sqrt(2)/2)
model$sense  <- c('>=', '>=', '=')

result <- gurobi(model)

print(result$objval)
print(result$x)
