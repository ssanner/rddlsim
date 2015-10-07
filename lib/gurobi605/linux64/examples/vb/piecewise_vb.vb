' Copyright 2014, Gurobi Optimization, Inc.

' This example considers the following separable, convex problem:
'
'    minimize    f(x) - y + g(z)
'    subject to  x + 2 y + 3 z <= 4
'                x +   y       >= 1
'                x,    y,    z <= 1
'
' where f(u) = exp(-u) and g(u) = 2 u^2 - 4 u, for all real u. It
' formulates and solves a simpler LP model by approximating f and
' g with piecewise-linear functions. Then it transforms the model
' into a MIP by negating the approximation for f, which corresponds
' to a non-convex piecewise-linear function, and solves it again.

Imports System
Imports Gurobi

Class piecewise_vb
    Shared Function f(u As Double) As Double
        Return Math.Exp(-u)
    End Function
    Shared Function g(u As Double) As Double
        Return 2 * u * u - 4 * u
    End Function

    Shared Sub Main()
        Try
            ' Create environment

            Dim env As New GRBEnv()

            ' Create a new model

            Dim model As New GRBModel(env)

            ' Create variables

            Dim lb As Double = 0.0, ub As Double = 1.0

            Dim x As GRBVar = model.AddVar(lb, ub, 0.0, GRB.CONTINUOUS, "x")
            Dim y As GRBVar = model.AddVar(lb, ub, 0.0, GRB.CONTINUOUS, "y")
            Dim z As GRBVar = model.AddVar(lb, ub, 0.0, GRB.CONTINUOUS, "z")

            ' Integrate new variables

            model.Update()

            ' Set objective for y

            model.SetObjective(-y)

            ' Add piecewise-linear objective functions for x and z

            Dim npts As Integer = 101
            Dim ptu As Double() = New Double(npts - 1) {}
            Dim ptf As Double() = New Double(npts - 1) {}
            Dim ptg As Double() = New Double(npts - 1) {}

            For i As Integer = 0 To npts - 1
                ptu(i) = lb + (ub - lb) * i / (npts - 1)
                ptf(i) = f(ptu(i))
                ptg(i) = g(ptu(i))
            Next

            model.SetPWLObj(x, ptu, ptf)
            model.SetPWLObj(z, ptu, ptg)

            ' Add constraint: x + 2 y + 3 z <= 4

            model.AddConstr(x + 2 * y + 3 * z <= 4.0, "c0")

            ' Add constraint: x + y >= 1

            model.AddConstr(x + y >= 1.0, "c1")

            ' Optimize model as an LP

            model.Optimize()

            Console.WriteLine("IsMIP: " & model.Get(GRB.IntAttr.IsMIP))

            Console.WriteLine(x.Get(GRB.StringAttr.VarName) & " " & _
                              x.Get(GRB.DoubleAttr.X))
            Console.WriteLine(y.Get(GRB.StringAttr.VarName) & " " & _
                              y.Get(GRB.DoubleAttr.X))
            Console.WriteLine(z.Get(GRB.StringAttr.VarName) & " " & _
                              z.Get(GRB.DoubleAttr.X))

            Console.WriteLine("Obj: " & model.Get(GRB.DoubleAttr.ObjVal))

            Console.WriteLine()

            ' Negate piecewise-linear objective function for x

            For i As Integer = 0 To npts - 1
                ptf(i) = -ptf(i)
            Next

            model.SetPWLObj(x, ptu, ptf)

            ' Optimize model as a MIP

            model.Optimize()

            Console.WriteLine("IsMIP: " & model.Get(GRB.IntAttr.IsMIP))

            Console.WriteLine(x.Get(GRB.StringAttr.VarName) & " " & _
                              x.Get(GRB.DoubleAttr.X))
            Console.WriteLine(y.Get(GRB.StringAttr.VarName) & " " & _
                              y.Get(GRB.DoubleAttr.X))
            Console.WriteLine(z.Get(GRB.StringAttr.VarName) & " " & _
                              z.Get(GRB.DoubleAttr.X))

            Console.WriteLine("Obj: " & model.Get(GRB.DoubleAttr.ObjVal))

            ' Dispose of model and environment

            model.Dispose()

            env.Dispose()
        Catch e As GRBException
            Console.WriteLine("Error code: " + e.ErrorCode & ". " + e.Message)
        End Try
    End Sub
End Class
