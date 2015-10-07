' Copyright 2014, Gurobi Optimization, Inc.
'
' Assign workers to shifts; each worker may or may not be available on a
' particular day. We use Pareto optimization to solve the model:
' first, we minimize the linear sum of the slacks. Then, we constrain
' the sum of the slacks, and we minimize a quadratic objective that
' tries to balance the workload among the workers.

Imports System
Imports Gurobi

Class workforce4_vb
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

            ' Assignment variables: x(w)(s) == 1 if worker w is assigned
            ' to shift s. This is no longer a pure assignment model, so we
            ' must use binary variables.
            Dim x As GRBVar(,) = New GRBVar(nWorkers - 1, nShifts - 1) {}
            For w As Integer = 0 To nWorkers - 1
                For s As Integer = 0 To nShifts - 1
                    x(w, s) = model.AddVar(0, availability(w, s), 0, _
                                           GRB.BINARY, _
                                           Workers(w) & "." & Shifts(s))
                Next
            Next

            ' Add a new slack variable to each shift constraint so that the
            ' shifts can be satisfied
            Dim slacks As GRBVar() = New GRBVar(nShifts - 1) {}
            For s As Integer = 0 To nShifts - 1
                slacks(s) = _
                    model.AddVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, _
                                 Shifts(s) & "Slack")
            Next

            ' Variable to represent the total slack
            Dim totSlack As GRBVar = model.AddVar(0, GRB.INFINITY, 0, _
                                                  GRB.CONTINUOUS, "totSlack")

            ' Variables to count the total shifts worked by each worker
            Dim totShifts As GRBVar() = New GRBVar(nWorkers - 1) {}
            For w As Integer = 0 To nWorkers - 1
                totShifts(w) = _
                    model.AddVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, _
                                 Workers(w) & "TotShifts")
            Next

            ' Update model to integrate new variables
            model.Update()

            Dim lhs As GRBLinExpr

            ' Constraint: assign exactly shiftRequirements(s) workers
            ' to each shift s, plus the slack
            For s As Integer = 0 To nShifts - 1
                lhs = 0
                lhs.AddTerm(1.0, slacks(s))
                For w As Integer = 0 To nWorkers - 1
                    lhs.AddTerm(1.0, x(w, s))
                Next
                model.AddConstr(lhs = shiftRequirements(s), Shifts(s))
            Next

            ' Constraint: set totSlack equal to the total slack
            lhs = 0
            For s As Integer = 0 To nShifts - 1
                lhs.AddTerm(1.0, slacks(s))
            Next
            model.AddConstr(lhs = totSlack, "totSlack")

            ' Constraint: compute the total number of shifts for each worker
            For w As Integer = 0 To nWorkers - 1
                lhs = 0
                For s As Integer = 0 To nShifts - 1
                    lhs.AddTerm(1.0, x(w, s))
                Next
                model.AddConstr(lhs = totShifts(w), "totShifts" & Workers(w))
            Next

            ' Objective: minimize the total slack
            model.SetObjective(1.0*totSlack)

            ' Optimize
            Dim status As Integer = _
                solveAndPrint(model, totSlack, nWorkers, Workers, totShifts)
            If status <> GRB.Status.OPTIMAL Then
                Exit Sub
            End If

            ' Constrain the slack by setting its upper and lower bounds
            totSlack.Set(GRB.DoubleAttr.UB, totSlack.Get(GRB.DoubleAttr.X))
            totSlack.Set(GRB.DoubleAttr.LB, totSlack.Get(GRB.DoubleAttr.X))

            ' Variable to count the average number of shifts worked
            Dim avgShifts As GRBVar = model.AddVar(0, GRB.INFINITY, 0, _
                                                  GRB.CONTINUOUS, "avgShifts")

            ' Variables to count the difference from average for each worker;
            ' note that these variables can take negative values.
            Dim diffShifts As GRBVar() = New GRBVar(nWorkers - 1) {}
            For w As Integer = 0 To nWorkers - 1
                diffShifts(w) = _
                    model.AddVar(-GRB.INFINITY, GRB.INFINITY, 0, _
                                 GRB.CONTINUOUS, Workers(w) & "Diff")
            Next

            ' Update model to integrate new variables
            model.Update()

            ' Constraint: compute the average number of shifts worked
            lhs = 0
            For w As Integer = 0 To nWorkers - 1
                lhs.AddTerm(1.0, totShifts(w))
            Next
            model.AddConstr(lhs = nWorkers * avgShifts, "avgShifts")

            ' Constraint: compute the difference from the average number of shifts
            For w As Integer = 0 To nWorkers - 1
                model.AddConstr(totShifts(w) - avgShifts = diffShifts(w), _
                                Workers(w) & "Diff")
            Next

            ' Objective: minimize the sum of the square of the difference
            ' from the average number of shifts worked
            Dim qobj As GRBQuadExpr = New GRBQuadExpr
            For w As Integer = 0 To nWorkers - 1
                qobj.AddTerm(1.0, diffShifts(w), diffShifts(w))
            Next
            model.SetObjective(qobj)

            ' Optimize
            status = solveAndPrint(model, totSlack, nWorkers, Workers, totShifts)
            If status <> GRB.Status.OPTIMAL Then
                Exit Sub
            End If

            ' Dispose of model and env
            model.Dispose()
            env.Dispose()

        Catch e As GRBException
            Console.WriteLine("Error code: " & e.ErrorCode & ". " & e.Message)
        End Try
    End Sub

    Private Shared Function solveAndPrint(ByVal model As GRBModel, _
                                          ByVal totSlack As GRBVar, _
                                          ByVal nWorkers As Integer, _
                                          ByVal Workers As String(), _
                                          ByVal totShifts As GRBVar()) As Integer
        model.Optimize()
        Dim status As Integer = model.Get(GRB.IntAttr.Status)
        solveAndPrint = status
        If (status = GRB.Status.INF_OR_UNBD) OrElse _
           (status = GRB.Status.INFEASIBLE) OrElse _
           (status = GRB.Status.UNBOUNDED) Then
            Console.WriteLine("The model cannot be solved because " & _
                              "it is infeasible or unbounded")
            Exit Function
        End If
        If status <> GRB.Status.OPTIMAL Then
            Console.WriteLine("Optimization was stopped with status " _
                              & status)
            Exit Function
        End If

        ' Print total slack and the number of shifts worked for each worker
        Console.WriteLine(vbLf & "Total slack required: " & _
                          totSlack.Get(GRB.DoubleAttr.X))
        For w As Integer = 0 To nWorkers - 1
            Console.WriteLine(Workers(w) & " worked " & _
                              totShifts(w).Get(GRB.DoubleAttr.X) & _
                              " shifts")
        Next

        Console.WriteLine(vbLf)
    End Function
End Class
