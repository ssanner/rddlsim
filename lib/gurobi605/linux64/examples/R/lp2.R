# Copyright 2014, Gurobi Optimization, Inc.
#
# Formulate a simple linear program, solve it, and then solve it
# again using the optimal basis.

library("gurobi")

model <- list()

model$A     <- matrix(c(1,3,4,8,2,3), nrow=2, byrow=T)
model$obj   <- c(1,2,3)
model$rhs   <- c(4,7)
model$sense <- c('>=', '>=')

# First solve - requires a few simplex iterations

result <- gurobi(model)

model$vbasis <- result$vbasis
model$cbasis <- result$cbasis

# Second solve - start from optimal basis, so no iterations

result <- gurobi(model)
