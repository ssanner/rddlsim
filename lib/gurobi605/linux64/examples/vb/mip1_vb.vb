' Copyright 2014, Gurobi Optimization, Inc.
'
' This example formulates and solves the following simple MIP model:
'
'    maximize    x +   y + 2 z
'    subject to  x + 2 y + 3 z <= 4
'                x +   y       >= 1
'    x, y, z binary

Imports System
Imports Gurobi

Class mip1_vb
    Shared Sub Main()
        Try
            Dim env As GRBEnv = New GRBEnv("mip1.log")
            Dim model As GRBModel = New GRBModel(env)

            ' Create variables

            Dim x As GRBVar = model.AddVar(0.0, 1.0, 0.0, GRB.BINARY, "x")
            Dim y As GRBVar = model.AddVar(0.0, 1.0, 0.0, GRB.BINARY, "y")
            Dim z As GRBVar = model.AddVar(0.0, 1.0, 0.0, GRB.BINARY, "z")

            ' Integrate new variables

            model.Update()

            ' Set objective: maximize x + y + 2 z

            model.SetObjective(x + y + 2 * z, GRB.MAXIMIZE)

            ' Add constraint: x + 2 y + 3 z <= 4

            model.AddConstr(x + 2 * y + 3 * z <= 4.0, "c0")

            ' Add constraint: x + y >= 1

            model.AddConstr(x + 2 * y + 3 * z <= 4.0, "c1")

            ' Optimize model

            model.Optimize()

            Console.WriteLine(x.Get(GRB.StringAttr.VarName) & " " & _
                              x.Get(GRB.DoubleAttr.X))
            Console.WriteLine(y.Get(GRB.StringAttr.VarName) & " " & _
                              y.Get(GRB.DoubleAttr.X))
            Console.WriteLine(z.Get(GRB.StringAttr.VarName) & " " & _
                              z.Get(GRB.DoubleAttr.X))

            Console.WriteLine("Obj: " & model.Get(GRB.DoubleAttr.ObjVal))

            ' Dispose of model and env

            model.Dispose()
            env.Dispose()

        Catch e As GRBException
            Console.WriteLine("Error code: " & e.ErrorCode & ". " & e.Message)
        End Try
    End Sub
End Class
