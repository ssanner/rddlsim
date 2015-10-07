' Copyright 2014, Gurobi Optimization, Inc.
'
' Solve a traveling salesman problem on a randomly generated set of
' points using lazy constraints.   The base MIP model only includes
' 'degree-2' constraints, requiring each node to have exactly
' two incident edges.  Solutions to this model may contain subtours -
' tours that don't visit every node.  The lazy constraint callback
' adds new constraints to cut them off.

Imports Gurobi

Class tsp_vb
    Inherits GRBCallback
    Private vars As GRBVar(,)

    Public Sub New(xvars As GRBVar(,))
        vars = xvars
    End Sub

    ' Subtour elimination callback.    Whenever a feasible solution is found,
    ' find the smallest subtour, and add a subtour elimination constraint
    ' if the tour doesn't visit every node.

    Protected Overrides Sub Callback()
        Try
            If where = GRB.Callback.MIPSOL Then
                ' Found an integer feasible solution - does it visit every node?

                Dim n As Integer = vars.GetLength(0)
                Dim tour As Integer() = findsubtour(GetSolution(vars))

                If tour.Length < n Then
                    ' Add subtour elimination constraint
                    Dim expr As GRBLinExpr = 0
                    For i As Integer = 0 To tour.Length - 1
                        For j As Integer = i + 1 To tour.Length - 1
                            expr.AddTerm(1.0, vars(tour(i), tour(j)))
                        Next
                    Next
                    AddLazy(expr <= tour.Length - 1)
                End If
            End If
        Catch e As GRBException
            Console.WriteLine("Error code: " & e.ErrorCode & ". " & e.Message)
            Console.WriteLine(e.StackTrace)
        End Try
    End Sub

    ' Given an integer-feasible solution 'sol', returns the smallest
    ' sub-tour (as a list of node indices).

    Protected Shared Function findsubtour(sol As Double(,)) As Integer()
        Dim n As Integer = sol.GetLength(0)
        Dim seen As Boolean() = New Boolean(n - 1) {}
        Dim tour As Integer() = New Integer(n - 1) {}
        Dim bestind As Integer, bestlen As Integer
        Dim i As Integer, node As Integer, len As Integer, start As Integer

        For i = 0 To n - 1
            seen(i) = False
        Next

        start = 0
        bestlen = n+1
        bestind = -1
        node = 0
        While start < n
            For node = 0 To n - 1
                if Not seen(node)
                    Exit For
                End if
            Next
            if node = n
                Exit While
            End if
            For len = 0 To n - 1
                tour(start+len) = node
                seen(node) = true
                For i = 0 To n - 1
                    if sol(node, i) > 0.5 AndAlso Not seen(i)
                        node = i
                        Exit For
                    End If
                Next
                If i = n
                    len = len + 1
                    If len < bestlen
                        bestlen = len
                        bestind = start
                    End If
                    start = start + len
                    Exit For
                End If
            Next
        End While

        For i = 0 To bestlen - 1
          tour(i) = tour(bestind+i)
        Next
        System.Array.Resize(tour, bestlen)

        Return tour
    End Function

    ' Euclidean distance between points 'i' and 'j'

    Protected Shared Function distance(x As Double(), y As Double(), _
                                       i As Integer, j As Integer) As Double
        Dim dx As Double = x(i) - x(j)
        Dim dy As Double = y(i) - y(j)
        Return Math.Sqrt(dx * dx + dy * dy)
    End Function

    Public Shared Sub Main(args As String())

        If args.Length < 1 Then
            Console.WriteLine("Usage: tsp_vb nnodes")
            Return
        End If

        Dim n As Integer = Convert.ToInt32(args(0))

        Try
            Dim env As New GRBEnv()
            Dim model As New GRBModel(env)

            ' Must set LazyConstraints parameter when using lazy constraints

            model.GetEnv().Set(GRB.IntParam.LazyConstraints, 1)

            Dim x As Double() = New Double(n - 1) {}
            Dim y As Double() = New Double(n - 1) {}

            Dim r As New Random()
            For i As Integer = 0 To n - 1
                x(i) = r.NextDouble()
                y(i) = r.NextDouble()
            Next

            ' Create variables

            Dim vars As GRBVar(,) = New GRBVar(n - 1, n - 1) {}

            For i As Integer = 0 To n - 1
                For j As Integer = 0 To i
                    vars(i, j) = model.AddVar(0.0, 1.0, distance(x, y, i, j), _
                                              GRB.BINARY, "x" & i & "_" & j)
                    vars(j, i) = vars(i, j)
                Next
            Next

            ' Integrate variables

            model.Update()

            ' Degree-2 constraints

            For i As Integer = 0 To n - 1
                Dim expr As GRBLinExpr = 0
                For j As Integer = 0 To n - 1
                    expr.AddTerm(1.0, vars(i, j))
                Next
                model.AddConstr(expr = 2.0, "deg2_" & i)
            Next

            ' Forbid edge from node back to itself

            For i As Integer = 0 To n - 1
                vars(i, i).Set(GRB.DoubleAttr.UB, 0.0)
            Next

            model.SetCallback(New tsp_vb(vars))
            model.Optimize()

            If model.Get(GRB.IntAttr.SolCount) > 0 Then
                Dim tour As Integer() = findsubtour(model.Get(GRB.DoubleAttr.X, vars))

                Console.Write("Tour: ")
                For i As Integer = 0 To tour.Length - 1
                    Console.Write(tour(i) & " ")
                Next
                Console.WriteLine()
            End If

            ' Dispose of model and environment
            model.Dispose()

            env.Dispose()
        Catch e As GRBException
            Console.WriteLine("Error code: " & e.ErrorCode & ". " & e.Message)
            Console.WriteLine(e.StackTrace)
        End Try
    End Sub
End Class
