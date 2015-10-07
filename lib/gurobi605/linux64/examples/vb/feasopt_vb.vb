' Copyright 2014, Gurobi Optimization, Inc.
'
' This example reads a MIP model from a file, adds artificial
' variables to each constraint, and then minimizes the sum of the
' artificial variables.  A solution with objective zero corresponds
' to a feasible solution to the input model.
' We can also use FeasRelax feature to do it. In this example, we
' use minrelax=1, i.e. optimizing the returned model finds a solution
' that minimizes the original objective, but only from among those
' solutions that minimize the sum of the artificial variables.

Imports Gurobi
Imports System

Class feasopt_vb
    Shared Sub Main(ByVal args As String())

        If args.Length < 1 Then
            Console.WriteLine("Usage: feasopt_vb filename")
            Return
        End If

        Try
            Dim env As New GRBEnv()
            Dim feasmodel As New GRBModel(env, args(0))

            'Create a copy to use FeasRelax feature later
            Dim feasmodel1 As New GRBModel(feasmodel)

            ' Clear objective
            feasmodel.SetObjective(New GRBLinExpr())

            ' Add slack variables
            Dim c As GRBConstr() = feasmodel.GetConstrs()
            For i As Integer = 0 To c.Length - 1
                Dim sense As Char = c(i).Get(GRB.CharAttr.Sense)
                If sense <> ">"c Then
                    Dim constrs As GRBConstr() = New GRBConstr() {c(i)}
                    Dim coeffs As Double() = New Double() {-1}
                    feasmodel.AddVar(0.0, GRB.INFINITY, 1.0, GRB.CONTINUOUS, _
                                     constrs, coeffs, _
                                     "ArtN_" & c(i).Get(GRB.StringAttr.ConstrName))
                End If
                If sense <> "<"c Then
                    Dim constrs As GRBConstr() = New GRBConstr() {c(i)}
                    Dim coeffs As Double() = New Double() {1}
                    feasmodel.AddVar(0.0, GRB.INFINITY, 1.0, GRB.CONTINUOUS, _
                                     constrs, coeffs, _
                                     "ArtP_" & c(i).Get(GRB.StringAttr.ConstrName))
                End If
            Next
            feasmodel.Update()

            ' Optimize modified model
            feasmodel.Write("feasopt.lp")
            feasmodel.Optimize()

            ' Use FeasRelax feature */
            feasmodel1.FeasRelax(GRB.FEASRELAX_LINEAR, true, false, true)
            feasmodel1.Write("feasopt1.lp")
            feasmodel1.Optimize()

            ' Dispose of model and env
            feasmodel1.Dispose()
            feasmodel.Dispose()
            env.Dispose()

        Catch e As GRBException
            Console.WriteLine("Error code: " & e.ErrorCode & ". " & e.Message)
        End Try
    End Sub
End Class
