' Copyright 2014, Gurobi Optimization, Inc.

' Assign workers to shifts; each worker may or may not be available on a
' particular day. If the problem cannot be solved, relax the model
' to determine which constraints cannot be satisfied, and how much
' they need to be relaxed.

Imports System
Imports Gurobi

Class workforce3_vb
    Shared Sub Main()
        Try

            ' Sample data
            ' Sets of days and workers
            Dim Shifts As String() = New String() {"Mon1", "Tue2", "Wed3", "Thu4", _
                                                   "Fri5", "Sat6", "Sun7", "Mon8", _
                                                   "Tue9", "Wed10", "Thu11", _
                                                   "Fri12", "Sat13", "Sun14"}
            Dim Workers As String() = New String() {"Amy", "Bob", "Cathy", "Dan", _
                                                    "Ed", "Fred", "Gu"}

            Dim nShifts As Integer = Shifts.Length
            Dim nWorkers As Integer = Workers.Length

            ' Number of workers required for each shift
            Dim shiftRequirements As Double() = New Double() {3, 2, 4, 4, 5, 6, _
                                                              5, 2, 2, 3, 4, 6, _
                                                              7, 5}

            ' Amount each worker is paid to work one shift
            Dim pay As Double() = New Double() {10, 12, 10, 8, 8, 9, 11}

            ' Worker availability: 0 if the worker is unavailable for a shift
            Dim availability As Double(,) = New Double(,) { _
                        {0, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1}, _
                        {1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0}, _
                        {0, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1}, _
                        {0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1}, _
                        {1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1}, _
                        {1, 1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 1}, _
                        {1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}}

            ' Model
            Dim env As New GRBEnv()
            Dim model As New GRBModel(env)
            model.Set(GRB.StringAttr.ModelName, "assignment")

            ' Assignment variables: x[w][s] == 1 if worker w is assigned
            ' to shift s. Since an assignment model always produces integer
            ' solutions, we use continuous variables and solve as an LP.
            Dim x As GRBVar(,) = New GRBVar(nWorkers - 1, nShifts - 1) {}
            For w As Integer = 0 To nWorkers - 1
                For s As Integer = 0 To nShifts - 1
                    x(w, s) = model.AddVar(0, availability(w, s), pay(w), _
                                           GRB.CONTINUOUS, _
                                           Workers(w) & "." & Shifts(s))
                Next
            Next

            ' The objective is to minimize the total pay costs
            model.Set(GRB.IntAttr.ModelSense, 1)

            ' Update model to integrate new variables
            model.Update()

            ' Constraint: assign exactly shiftRequirements[s] workers
            ' to each shift s
            For s As Integer = 0 To nShifts - 1
                Dim lhs As GRBLinExpr = 0.0
                For w As Integer = 0 To nWorkers - 1
                    lhs.AddTerm(1.0, x(w, s))
                Next
                model.AddConstr(lhs = shiftRequirements(s), Shifts(s))
            Next

            ' Optimize
            model.Optimize()
            Dim status As Integer = model.Get(GRB.IntAttr.Status)
            If status = GRB.Status.UNBOUNDED Then
                Console.WriteLine("The model cannot be solved " & _
                                  "because it is unbounded")
                Return
            End If
            If status = GRB.Status.OPTIMAL Then
                Console.WriteLine("The optimal objective is " & _
                                  model.Get(GRB.DoubleAttr.ObjVal))
                Return
            End If
            If (status <> GRB.Status.INF_OR_UNBD) AndAlso _
               (status <> GRB.Status.INFEASIBLE) Then
                Console.WriteLine("Optimization was stopped with status " & _
                                  status)
                Return
            End If

            ' Relax the constraints to make the model feasible
            Console.WriteLine("The model is infeasible; relaxing the constraints")
            Dim orignumvars As Integer = model.Get(GRB.IntAttr.NumVars)
            model.FeasRelax(0, False, False, True)
            model.Optimize()
            status = model.Get(GRB.IntAttr.Status)
            If (status = GRB.Status.INF_OR_UNBD) OrElse _
               (status = GRB.Status.INFEASIBLE) OrElse _
               (status = GRB.Status.UNBOUNDED) Then
                Console.WriteLine("The relaxed model cannot be solved " & _
                                  "because it is infeasible or unbounded")
                Return
            End If
            If status <> GRB.Status.OPTIMAL Then
                Console.WriteLine("Optimization was stopped with status " & status)
                Return
            End If

            Console.WriteLine(vbLf & "Slack values:")
            Dim vars As GRBVar() = model.GetVars()
            For i As Integer = orignumvars To model.Get(GRB.IntAttr.NumVars) - 1
                Dim sv As GRBVar = vars(i)
                If sv.Get(GRB.DoubleAttr.X) > 1E-06 Then
                    Console.WriteLine(sv.Get(GRB.StringAttr.VarName) & " = " & _
                                      sv.Get(GRB.DoubleAttr.X))
                End If
            Next

            ' Dispose of model and environment
            model.Dispose()

            env.Dispose()
        Catch e As GRBException
            Console.WriteLine("Error code: " + e.ErrorCode & ". " + e.Message)
        End Try
    End Sub
End Class
