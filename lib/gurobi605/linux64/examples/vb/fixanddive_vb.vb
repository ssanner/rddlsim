' Copyright 2014, Gurobi Optimization, Inc.
'
' Implement a simple MIP heuristic.  Relax the model,
' sort variables based on fractionality, and fix the 25% of
' the fractional variables that are closest to integer variables.
' Repeat until either the relaxation is integer feasible or
' linearly infeasible.

Imports System
Imports System.Collections.Generic
Imports Gurobi

Class fixanddive_vb
    ' Comparison class used to sort variable list based on relaxation
    ' fractionality

    Private Class FractionalCompare : Implements IComparer(Of GRBVar)
        Public Function Compare(ByVal v1 As GRBVar, ByVal v2 As GRBVar) As Integer _
                             Implements IComparer(Of Gurobi.GRBVar).Compare
            Try
                Dim sol1 As Double = Math.Abs(v1.Get(GRB.DoubleAttr.X))
                Dim sol2 As Double = Math.Abs(v2.Get(GRB.DoubleAttr.X))
                Dim frac1 As Double = Math.Abs(sol1 - Math.Floor(sol1 + 0.5))
                Dim frac2 As Double = Math.Abs(sol2 - Math.Floor(sol2 + 0.5))
                If frac1 < frac2 Then
                    Return -1
                ElseIf frac1 > frac2 Then
                    Return 1
                Else
                    Return 0
                End If
            Catch e As GRBException
                Console.WriteLine("Error code: " & e.ErrorCode & ". " & e.Message)
            End Try
            Return 0
        End Function
    End Class

    Shared Sub Main(ByVal args As String())

        If args.Length < 1 Then
            Console.WriteLine("Usage: fixanddive_vb filename")
            Return
        End If

        Try
            ' Read model
            Dim env As New GRBEnv()
            Dim model As New GRBModel(env, args(0))

            ' Collect integer variables and relax them
            Dim intvars As New List(Of GRBVar)()
            For Each v As GRBVar In model.GetVars()
                If v.Get(GRB.CharAttr.VType) <> GRB.CONTINUOUS Then
                    intvars.Add(v)
                    v.Set(GRB.CharAttr.VType, GRB.CONTINUOUS)
                End If
            Next

            model.GetEnv().Set(GRB.IntParam.OutputFlag, 0)
            model.Optimize()

            ' Perform multiple iterations. In each iteration, identify the first
            ' quartile of integer variables that are closest to an integer value
            ' in the relaxation, fix them to the nearest integer, and repeat.

            For iter As Integer = 0 To 999

                ' create a list of fractional variables, sorted in order of
                ' increasing distance from the relaxation solution to the nearest
                ' integer value

                Dim fractional As New List(Of GRBVar)()
                For Each v As GRBVar In intvars
                    Dim sol As Double = Math.Abs(v.Get(GRB.DoubleAttr.X))
                    If Math.Abs(sol - Math.Floor(sol + 0.5)) > 0.00001 Then
                        fractional.Add(v)
                    End If
                Next

                Console.WriteLine("Iteration " & iter & ", obj " & _
                                  model.Get(GRB.DoubleAttr.ObjVal) & _
                                  ", fractional " & fractional.Count)

                If fractional.Count = 0 Then
                    Console.WriteLine("Found feasible solution - objective " & _
                                      model.Get(GRB.DoubleAttr.ObjVal))
                    Exit For
                End If

                ' Fix the first quartile to the nearest integer value

                fractional.Sort(New FractionalCompare())
                Dim nfix As Integer = Math.Max(fractional.Count / 4, 1)
                For i As Integer = 0 To nfix - 1
                    Dim v As GRBVar = fractional(i)
                    Dim fixval As Double = Math.Floor(v.Get(GRB.DoubleAttr.X) + 0.5)
                    v.Set(GRB.DoubleAttr.LB, fixval)
                    v.Set(GRB.DoubleAttr.UB, fixval)
                    Console.WriteLine("  Fix " & v.Get(GRB.StringAttr.VarName) & _
                                      " to " & fixval & _
                                      " ( rel " & v.Get(GRB.DoubleAttr.X) & " )")
                Next

                model.Optimize()

                ' Check optimization result

                If model.Get(GRB.IntAttr.Status) <> GRB.Status.OPTIMAL Then
                    Console.WriteLine("Relaxation is infeasible")
                    Exit For
                End If
            Next

            ' Dispose of model and env
            model.Dispose()
            env.Dispose()

        Catch e As GRBException
            Console.WriteLine("Error code: " & e.ErrorCode & ". " + e.Message)
        End Try
    End Sub
End Class
