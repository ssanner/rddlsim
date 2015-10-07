' Copyright 2014, Gurobi Optimization, Inc.
'
' This example creates a very simple Special Ordered Set (SOS) model.
' The model consists of 3 continuous variables, no linear constraints,
' and a pair of SOS constraints of type 1.

Imports System
Imports Gurobi

Class sos_vb
    Shared Sub Main()
        Try
            Dim env As New GRBEnv()
            Dim model As New GRBModel(env)

            ' Create variables

            Dim ub As Double() = {1, 1, 2}
            Dim obj As Double() = {-2, -1, -1}
            Dim names As String() = {"x0", "x1", "x2"}

            Dim x As GRBVar() = model.AddVars(Nothing, ub, obj, Nothing, names)

            ' Integrate new variables

            model.Update()

            ' Add first SOS1: x0=0 or x1=0

            Dim sosv1 As GRBVar() = {x(0), x(1)}
            Dim soswt1 As Double() = {1, 2}

            model.AddSOS(sosv1, soswt1, GRB.SOS_TYPE1)

            ' Add second SOS1: x0=0 or x2=0

            Dim sosv2 As GRBVar() = {x(0), x(2)}
            Dim soswt2 As Double() = {1, 2}

            model.AddSOS(sosv2, soswt2, GRB.SOS_TYPE1)

            ' Optimize model

            model.Optimize()

            For i As Integer = 0 To 2
                Console.WriteLine(x(i).Get(GRB.StringAttr.VarName) & " " & _
                                  x(i).Get(GRB.DoubleAttr.X))
            Next

            ' Dispose of model and env
            model.Dispose()
            env.Dispose()

        Catch e As GRBException
            Console.WriteLine("Error code: " & e.ErrorCode & ". " & e.Message)
        End Try
    End Sub
End Class
