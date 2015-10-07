# Copyright 2014, Gurobi Optimization, Inc.
#
# This example formulates and solves the following simple LP model:
#  maximize
#        x + 2 y + 3 z
#  subject to
#        x +   y       <= 1
#              y +   z <= 1

library("Matrix")
library("gurobi")

model <- list()

model$A          <- matrix(c(1,1,0,0,1,1), nrow=2, byrow=T)
model$obj        <- c(1,1,2)
model$modelsense <- "max"
model$rhs        <- c(1,1)
model$sense      <- c('<=', '<=')

result <- gurobi(model)

print(result$objval)
print(result$x)

# Second option for A - as a sparseMatrix (using the Matrix package)...

model$A <- spMatrix(2, 3, c(1, 1, 2, 2), c(1, 2, 2, 3), c(1, 1, 1, 1))

params <- list(Method=2, TimeLimit=100)

result <- gurobi(model, params)

print(result$objval)
print(result$x)

# Third option for A - as a sparse triplet matrix (using the slam package)...

model$A <- simple_triplet_matrix(c(1, 1, 2, 2), c(1, 2, 2, 3), c(1, 1, 1, 1))

params <- list(Method=2, TimeLimit=100)

result <- gurobi(model, params)

print(result$objval)
print(result$x)
