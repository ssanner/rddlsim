' Copyright 2014, Gurobi Optimization, Inc.

' This example formulates and solves the following simple QCP model:
'
'     maximize    x
'     subject to  x + y + z = 1
'                 x^2 + y^2 <= z^2 (second-order cone)
'                 x^2 <= yz        (rotated second-order cone)

Imports Gurobi

Class qcp_vb
    Shared Sub Main()
        Try
            Dim env As New GRBEnv("qcp.log")
            Dim model As New GRBModel(env)

            ' Create variables

            Dim x As GRBVar = model.AddVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "x")
            Dim y As GRBVar = model.AddVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "y")
            Dim z As GRBVar = model.AddVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "z")

            ' Integrate new variables

            model.Update()

            ' Set objective

            Dim obj As GRBLinExpr = x
            model.SetObjective(obj, GRB.MAXIMIZE)

            ' Add linear constraint: x + y + z = 1

            model.AddConstr(x + y + z = 1.0, "c0")

            ' Add second-order cone: x^2 + y^2 <= z^2

            model.AddQConstr(x * x + y * y <= z * z, "qc0")

            ' Add rotated cone: x^2 <= yz

            model.AddQConstr(x * x <= y * z, "qc1")

            ' Optimize model

            model.Optimize()

            Console.WriteLine(x.Get(GRB.StringAttr.VarName) & " " & _
                              x.Get(GRB.DoubleAttr.X))
            Console.WriteLine(y.Get(GRB.StringAttr.VarName) & " " & _
                              y.Get(GRB.DoubleAttr.X))
            Console.WriteLine(z.Get(GRB.StringAttr.VarName) & " " & _
                              z.Get(GRB.DoubleAttr.X))

            Console.WriteLine("Obj: " & model.Get(GRB.DoubleAttr.ObjVal) & " " & _
                              obj.Value)

            ' Dispose of model and env

            model.Dispose()

            env.Dispose()
        Catch e As GRBException
            Console.WriteLine("Error code: " & e.ErrorCode & ". " & e.Message)
        End Try
    End Sub
End Class
