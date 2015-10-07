' Copyright 2014, Gurobi Optimization, Inc.
'
' Sudoku example.
'
' The Sudoku board is a 9x9 grid, which is further divided into a 3x3 grid
' of 3x3 grids.  Each cell in the grid must take a value from 0 to 9.
' No two grid cells in the same row, column, or 3x3 subgrid may take the
' same value.

' In the MIP formulation, binary variables x(i,j,v) indicate whether
' cell <i,j> takes value 'v'.  The constraints are as follows:
'   1. Each cell must take exactly one value (sum_v x(i,j,v) = 1)
'   2. Each value is used exactly once per row (sum_i x(i,j,v) = 1)
'   3. Each value is used exactly once per column (sum_j x(i,j,v) = 1)
'   4. Each value is used exactly once per 3x3 subgrid (sum_grid x(i,j,v) = 1)
'
' Input datasets for this example can be found in examples/data/sudoku*.

Imports System
Imports System.IO
Imports Gurobi

Class sudoku_vb
    Shared Sub Main(ByVal args as String())
        Dim n As Integer = 9
        Dim s As Integer = 3

        If args.Length < 1 Then
            Console.WriteLine("Usage: sudoku_vb filename")
            Return
        End If

        Try
            Dim env As New GRBEnv()
            Dim model As New GRBModel(env)

            ' Create 3-D array of model variables

            Dim vars As GRBVar(,,) = New GRBVar(n - 1, n - 1, n - 1) {}

            For i As Integer = 0 To n - 1
                For j As Integer = 0 To n - 1
                    For v As Integer = 0 To n - 1
                        Dim st As String = "G_" & i & "_" & j & "_" & v
                        vars(i, j, v) = model.AddVar(0.0, 1.0, 0.0, GRB.BINARY, st)
                    Next
                Next
            Next

            ' Integrate variables into model

            model.Update()

            ' Add constraints

            Dim expr As GRBLinExpr

            ' Each cell must take one value

            For i As Integer = 0 To n - 1
                For j As Integer = 0 To n - 1
                    expr = 0
                    For v As Integer = 0 To n - 1
                        expr.AddTerm(1.0, vars(i, j, v))
                    Next
                    Dim st As String = "V_" & i & "_" & j
                    model.AddConstr(expr = 1, st)
                Next
            Next

            ' Each value appears once per row

            For i As Integer = 0 To n - 1
                For v As Integer = 0 To n - 1
                    expr = 0
                    For j As Integer = 0 To n - 1
                        expr.AddTerm(1.0, vars(i, j, v))
                    Next
                    Dim st As String = "R_" & i & "_" & v
                    model.AddConstr(expr = 1, st)
                Next
            Next

            ' Each value appears once per column

            For j As Integer = 0 To n - 1
                For v As Integer = 0 To n - 1
                    expr = 0
                    For i As Integer = 0 To n - 1
                        expr.AddTerm(1.0, vars(i, j, v))
                    Next
                    Dim st As String = "C_" & j & "_" & v
                    model.AddConstr(expr = 1, st)
                Next
            Next

            ' Each value appears once per sub-grid

            For v As Integer = 0 To n - 1
                For i0 As Integer = 0 To s - 1
                    For j0 As Integer = 0 To s - 1
                        expr = 0
                        For i1 As Integer = 0 To s - 1
                            For j1 As Integer = 0 To s - 1
                                expr.AddTerm(1.0, vars(i0 * s + i1, j0 * s + j1, v))
                            Next
                        Next
                        Dim st As String = "Sub_" & v & "_" & i0 & "_" & j0
                        model.AddConstr(expr = 1, st)
                    Next
                Next
            Next

            ' Update model

            model.Update()

            ' Fix variables associated with pre-specified cells

            Dim sr As StreamReader = File.OpenText(args(0))

            For i As Integer = 0 To n - 1
                Dim input As String = sr.ReadLine()
                For j As Integer = 0 To n - 1
                    Dim val As Integer = Microsoft.VisualBasic.Asc(input(j)) - 48 - 1
                    ' 0-based
                    If val >= 0 Then
                        vars(i, j, val).Set(GRB.DoubleAttr.LB, 1.0)
                    End If
                Next
            Next

            ' Optimize model

            model.Optimize()

            ' Write model to file
            model.Write("sudoku.lp")

            Dim x As Double(,,) = model.Get(GRB.DoubleAttr.X, vars)

            Console.WriteLine()
            For i As Integer = 0 To n - 1
                For j As Integer = 0 To n - 1
                    For v As Integer = 0 To n - 1
                        If x(i, j, v) > 0.5 Then
                            Console.Write(v + 1)
                        End If
                    Next
                Next
                Console.WriteLine()
            Next

            ' Dispose of model and env
            model.Dispose()
            env.Dispose()

        Catch e As GRBException
            Console.WriteLine("Error code: " & e.ErrorCode & ". " & e.Message)
        End Try
    End Sub
End Class
