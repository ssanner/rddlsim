' Copyright 2014, Gurobi Optimization, Inc.
'
' This example reads an LP model from a file and solves it.
' If the model is infeasible or unbounded, the example turns off
' presolve and solves the model again. If the model is infeasible,
' the example computes an Irreducible Inconsistent Subsystem (IIS),
' and writes it to a file.

Imports System
Imports Gurobi

Class lp_vb
    Shared Sub Main(ByVal args As String())

        If args.Length < 1 Then
            Console.WriteLine("Usage: lp_vb filename")
            Return
        End If

        Try
            Dim env As GRBEnv = New GRBEnv("lp1.log")
            Dim model As GRBModel = New GRBModel(env, args(0))

            model.Optimize()

            Dim optimstatus As Integer = model.Get(GRB.IntAttr.Status)

            If optimstatus = GRB.Status.INF_OR_UNBD Then
                model.GetEnv().Set(GRB.IntParam.Presolve, 0)
                model.Optimize()
                optimstatus = model.Get(GRB.IntAttr.Status)
            End If

            If optimstatus = GRB.Status.OPTIMAL Then
                Dim objval As Double = model.Get(GRB.DoubleAttr.ObjVal)
                Console.WriteLine("Optimal objective: " & objval)
            ElseIf optimstatus = GRB.Status.INFEASIBLE Then
                Console.WriteLine("Model is infeasible")
                model.ComputeIIS()
                model.Write("model.ilp")
            ElseIf optimstatus = GRB.Status.UNBOUNDED Then
                Console.WriteLine("Model is unbounded")
            Else
                Console.WriteLine("Optimization was stopped with status = " & _
                                  optimstatus)
            End If

            ' Dispose of model and env
            model.Dispose()
            env.Dispose()

        Catch e As GRBException
            Console.WriteLine("Error code: " & e.ErrorCode & ". " & e.Message)
        End Try
    End Sub
End Class
