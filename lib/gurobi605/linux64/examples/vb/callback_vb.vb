' Copyright 2014, Gurobi Optimization, Inc.

'   This example reads a model from a file, sets up a callback that
'   monitors optimization progress and implements a custom
'   termination strategy, and outputs progress information to the
'   screen and to a log file.
'
'   The termination strategy implemented in this callback stops the
'   optimization of a MIP model once at least one of the following two
'   conditions have been satisfied:
'     1) The optimality gap is less than 10%
'     2) At least 10000 nodes have been explored, and an integer feasible
'        solution has been found.
'   Note that termination is normally handled through Gurobi parameters
'   (MIPGap, NodeLimit, etc.).  You should only use a callback for
'   termination if the available parameters don't capture your desired
'   termination criterion.

Imports System
Imports Gurobi

Class callback_vb
    Inherits GRBCallback
    Private vars As GRBVar()
    Private lastnode As Double
    Private lastiter As Double

    Public Sub New(ByVal xvars As GRBVar())
        vars = xvars
        lastnode = lastiter = -1
    End Sub

    Protected Overloads Overrides Sub Callback()
        Try
            If where = GRB.Callback.PRESOLVE Then
                ' Presolve callback
                Dim cdels As Integer = GetIntInfo(GRB.Callback.PRE_COLDEL)
                Dim rdels As Integer = GetIntInfo(GRB.Callback.PRE_ROWDEL)
                Console.WriteLine(cdels & " columns and " & rdels & " rows are removed")
            ElseIf where = GRB.Callback.SIMPLEX Then
                ' Simplex callback
                Dim itcnt As Double = GetDoubleInfo(GRB.Callback.SPX_ITRCNT)
                If itcnt Mod - lastiter >= 100 Then
                    lastiter = itcnt
                    Dim obj As Double = GetDoubleInfo(GRB.Callback.SPX_OBJVAL)
                    Dim pinf As Double = GetDoubleInfo(GRB.Callback.SPX_PRIMINF)
                    Dim dinf As Double = GetDoubleInfo(GRB.Callback.SPX_DUALINF)
                    Dim ispert As Integer = GetIntInfo(GRB.Callback.SPX_ISPERT)
                    Dim ch As Char
                    If ispert = 0 Then
                        ch = " "c
                    ElseIf ispert = 1 Then
                        ch = "S"c
                    Else
                        ch = "P"c
                    End If
                    Console.WriteLine(itcnt & "  " & obj & ch & "  " & pinf & "  " & dinf)
                End If
            ElseIf where = GRB.Callback.MIP Then
                ' General MIP callback
                Dim nodecnt As Double = GetDoubleInfo(GRB.Callback.MIP_NODCNT)
                If nodecnt - lastnode >= 100 Then
                    lastnode = nodecnt
                    Dim objbst As Double = GetDoubleInfo(GRB.Callback.MIP_OBJBST)
                    Dim objbnd As Double = GetDoubleInfo(GRB.Callback.MIP_OBJBND)
                    If Math.Abs(objbst - objbnd) < 0.1 * (1.0R + Math.Abs(objbst)) Then
                        Abort()
                    End If
                    Dim actnodes As Integer = CInt(GetDoubleInfo(GRB.Callback.MIP_NODLFT))
                    Dim itcnt As Integer = CInt(GetDoubleInfo(GRB.Callback.MIP_ITRCNT))
                    Dim solcnt As Integer = GetIntInfo(GRB.Callback.MIP_SOLCNT)
                    Dim cutcnt As Integer = GetIntInfo(GRB.Callback.MIP_CUTCNT)
                    Console.WriteLine(nodecnt & " " & actnodes & " " & itcnt & " " & _
                                      objbst & " " & objbnd & " " & solcnt & " " & cutcnt)
                End If
            ElseIf where = GRB.Callback.MIPSOL Then
                ' MIP solution callback
                Dim obj As Double = GetDoubleInfo(GRB.Callback.MIPSOL_OBJ)
                Dim nodecnt As Integer = CInt(GetDoubleInfo(GRB.Callback.MIPSOL_NODCNT))
                Dim x As Double() = GetSolution(vars)
                Console.WriteLine("**** New solution at node " & nodecnt & ", obj " & _
                                  obj & ", x(0) = " & x(0) & "****")
            End If
        Catch e As GRBException
            Console.WriteLine("Error code: " & e.ErrorCode & ". " & e.Message)
            Console.WriteLine(e.StackTrace)
        End Try
    End Sub

    Shared Sub Main(ByVal args As String())

        If args.Length < 1 Then
            Console.WriteLine("Usage: callback_vb filename")
            Return
        End If

        Try
            Dim env As New GRBEnv()
            Dim model As New GRBModel(env, args(0))

            Dim vars As GRBVar() = model.GetVars()

            ' Create a callback object and associate it with the model
            model.SetCallback(New callback_vb(vars))
            model.Optimize()

            Dim x As Double() = model.Get(GRB.DoubleAttr.X, vars)
            Dim vnames As String() = model.Get(GRB.StringAttr.VarName, vars)

            For j As Integer = 0 To vars.Length - 1
                If x(j) <> 0.0R Then
                    Console.WriteLine(vnames(j) & " " & x(j))
                End If
            Next

            ' Dispose of model and env
            model.Dispose()
            env.Dispose()

        Catch e As GRBException
            Console.WriteLine("Error code: " & e.ErrorCode & ". " & e.Message)
            Console.WriteLine(e.StackTrace)
        End Try
    End Sub
End Class
