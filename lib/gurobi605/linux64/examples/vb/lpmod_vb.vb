' Copyright 2014, Gurobi Optimization, Inc.
'
' This example reads an LP model from a file and solves it.
' If the model can be solved, then it finds the smallest positive variable,
' sets its upper bound to zero, and resolves the model two ways:
' first with an advanced start, then without an advanced start
' (i.e. from scratch).

Imports System
Imports Gurobi

Class lpmod_vb
    Shared Sub Main(ByVal args As String())

        If args.Length < 1 Then
            Console.WriteLine("Usage: lpmod_vb filename")
            Return
        End If

        Try
            ' Read model and determine whether it is an LP
            Dim env As New GRBEnv()
            Dim model As New GRBModel(env, args(0))
            If model.Get(GRB.IntAttr.IsMIP) <> 0 Then
                Console.WriteLine("The model is not a linear program")
                Environment.Exit(1)
            End If

            model.Optimize()

            Dim status As Integer = model.Get(GRB.IntAttr.Status)

            If (status = GRB.Status.INF_OR_UNBD) OrElse _
               (status = GRB.Status.INFEASIBLE) OrElse _
               (status = GRB.Status.UNBOUNDED) Then
                Console.WriteLine("The model cannot be solved because it is " & _
                                  "infeasible or unbounded")
                Environment.Exit(1)
            End If

            If status <> GRB.Status.OPTIMAL Then
                Console.WriteLine("Optimization was stopped with status " & status)
                Environment.Exit(0)
            End If

            ' Find the smallest variable value
            Dim minVal As Double = GRB.INFINITY
            Dim minVar As GRBVar = Nothing
            For Each v As GRBVar In model.GetVars()
                Dim sol As Double = v.Get(GRB.DoubleAttr.X)
                If (sol > 0.0001) AndAlso _
                   (sol < minVal) AndAlso _
                   (v.Get(GRB.DoubleAttr.LB) = 0.0) Then
                    minVal = sol
                    minVar = v
                End If
            Next

            Console.WriteLine(vbLf & "*** Setting " & _
                              minVar.Get(GRB.StringAttr.VarName) & _
                              " from " & minVal & " to zero ***" & vbLf)
            minVar.Set(GRB.DoubleAttr.UB, 0)

            ' Solve from this starting point
            model.Optimize()

            ' Save iteration & time info
            Dim warmCount As Double = model.Get(GRB.DoubleAttr.IterCount)
            Dim warmTime As Double = model.Get(GRB.DoubleAttr.Runtime)

            ' Reset the model and resolve
            Console.WriteLine(vbLf & "*** Resetting and solving " & _
                              "without an advanced start ***" & vbLf)
            model.Reset()
            model.Optimize()

            Dim coldCount As Double = model.Get(GRB.DoubleAttr.IterCount)
            Dim coldTime As Double = model.Get(GRB.DoubleAttr.Runtime)

            Console.WriteLine(vbLf & "*** Warm start: " & warmCount & _
                              " iterations, " & warmTime & " seconds")

            Console.WriteLine("*** Cold start: " & coldCount & " iterations, " & _
                              coldTime & " seconds")

            ' Dispose of model and env
            model.Dispose()
            env.Dispose()

        Catch e As GRBException
            Console.WriteLine("Error code: " & e.ErrorCode & ". " & e.Message)
        End Try
    End Sub
End Class
