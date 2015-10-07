' Copyright 2014, Gurobi Optimization, Inc.
'
' This example formulates and solves the following simple QP model:
'
'   minimize    x + y + x^2 + x*y + y^2 + y*z + z^2
'   subject to  x + 2 y + 3 z >= 4
'               x +   y       >= 1
'
' The example illustrates the use of dense matrices to store A and Q
' (and dense vectors for the other relevant data).  We don't recommend
' that you use dense matrices, but this example may be helpful if you
' already have your data in this format.

Imports Gurobi

Class dense_vb

    Protected Shared Function _
      dense_optimize(env As GRBEnv, _
                     rows As Integer, _
                     cols As Integer, _
                     c As Double(), _
                     Q As Double(,), _
                     A As Double(,), _
                     sense As Char(), _
                     rhs As Double(), _
                     lb As Double(), _
                     ub As Double(), _
                     vtype As Char(), _
                     solution As Double()) As Boolean

        Dim success As Boolean = False

        Try
            Dim model As New GRBModel(env)

            ' Add variables to the model

            Dim vars As GRBVar() = model.AddVars(lb, ub, Nothing, vtype, Nothing)
            model.Update()

            ' Populate A matrix

            For i As Integer = 0 To rows - 1
                Dim expr As New GRBLinExpr()
                For j As Integer = 0 To cols - 1
                    If A(i, j) <> 0 Then
                        expr.AddTerm(A(i, j), vars(j))
                    End If
                Next
                model.AddConstr(expr, sense(i), rhs(i), "")
            Next

            ' Populate objective

            Dim obj As New GRBQuadExpr()
            If Q IsNot Nothing Then
                For i As Integer = 0 To cols - 1
                    For j As Integer = 0 To cols - 1
                        If Q(i, j) <> 0 Then
                            obj.AddTerm(Q(i, j), vars(i), vars(j))
                        End If
                    Next
                Next
                For j As Integer = 0 To cols - 1
                    If c(j) <> 0 Then
                        obj.AddTerm(c(j), vars(j))
                    End If
                Next
                model.SetObjective(obj)
            End If

            ' Solve model

            model.Optimize()

            ' Extract solution

            If model.Get(GRB.IntAttr.Status) = GRB.Status.OPTIMAL Then
                success = True

                For j As Integer = 0 To cols - 1
                    solution(j) = vars(j).Get(GRB.DoubleAttr.X)
                Next
            End If

            model.Dispose()

        Catch e As GRBException
            Console.WriteLine("Error code: " & e.ErrorCode & ". " & e.Message)
        End Try

        Return success
    End Function

    Public Shared Sub Main(args As String())
        Try
            Dim env As New GRBEnv()

            Dim c As Double() = New Double() {1, 1, 0}
            Dim Q As Double(,) = New Double(,) {{1, 1, 0}, {0, 1, 1}, {0, 0, 1}}
            Dim A As Double(,) = New Double(,) {{1, 2, 3}, {1, 1, 0}}
            Dim sense As Char() = New Char() {">"C, ">"C}
            Dim rhs As Double() = New Double() {4, 1}
            Dim lb As Double() = New Double() {0, 0, 0}
            Dim success As Boolean
            Dim sol As Double() = New Double(2) {}

            success = dense_optimize(env, 2, 3, c, Q, A, sense, rhs, lb, Nothing, _
                                     Nothing, sol)

            If success Then
                Console.WriteLine("x: " & sol(0) & ", y: " & sol(1) & ", z: " & sol(2))
            End If

            ' Dispose of environment

            env.Dispose()

        Catch e As GRBException
            Console.WriteLine("Error code: " & e.ErrorCode & ". " & e.Message)
        End Try

    End Sub
End Class
