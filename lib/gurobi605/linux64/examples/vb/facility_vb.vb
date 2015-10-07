' Copyright 2014, Gurobi Optimization, Inc.
'
' Facility location: a company currently ships its product from 5 plants
' to 4 warehouses. It is considering closing some plants to reduce
' costs. What plant(s) should the company close, in order to minimize
' transportation and fixed costs?
'
' Based on an example from Frontline Systems:
' http://www.solver.com/disfacility.htm
' Used with permission.

Imports System
Imports Gurobi

Class facility_vb
    Shared Sub Main()
        Try

            ' Warehouse demand in thousands of units
            Dim Demand As Double() = New Double() {15, 18, 14, 20}

            ' Plant capacity in thousands of units
            Dim Capacity As Double() = New Double() {20, 22, 17, 19, 18}

            ' Fixed costs for each plant
            Dim FixedCosts As Double() = New Double() {12000, 15000, 17000, 13000, _
                                                       16000}

            ' Transportation costs per thousand units
            Dim TransCosts As Double(,) = New Double(,) {{4000, 2000, 3000, 2500, 4500}, _
                                                         {2500, 2600, 3400, 3000, 4000}, _
                                                         {1200, 1800, 2600, 4100, 3000}, _
                                                         {2200, 2600, 3100, 3700, 3200}}

            ' Number of plants and warehouses
            Dim nPlants As Integer = Capacity.Length
            Dim nWarehouses As Integer = Demand.Length

            ' Model
            Dim env As New GRBEnv()
            Dim model As New GRBModel(env)
            model.Set(GRB.StringAttr.ModelName, "facility")

            ' Plant open decision variables: open(p) == 1 if plant p is open.
            Dim open As GRBVar() = New GRBVar(nPlants - 1) {}
            For p As Integer = 0 To nPlants - 1
                open(p) = model.AddVar(0, 1, FixedCosts(p), GRB.BINARY, "Open" & p)
            Next

            ' Transportation decision variables: how much to transport from
            ' a plant p to a warehouse w
            Dim transport As GRBVar(,) = New GRBVar(nWarehouses - 1, nPlants - 1) {}
            For w As Integer = 0 To nWarehouses - 1
                For p As Integer = 0 To nPlants - 1
                    transport(w, p) = model.AddVar(0, GRB.INFINITY, _
                                                   TransCosts(w, p), GRB.CONTINUOUS, _
                                                   "Trans" & p & "." & w)
                Next
            Next

            ' The objective is to minimize the total fixed and variable costs
            model.Set(GRB.IntAttr.ModelSense, 1)

            ' Update model to integrate new variables
            model.Update()

            ' Production constraints
            ' Note that the right-hand limit sets the production to zero if
            ' the plant is closed
            For p As Integer = 0 To nPlants - 1
                Dim ptot As GRBLinExpr = 0
                For w As Integer = 0 To nWarehouses - 1
                    ptot.AddTerm(1.0, transport(w, p))
                Next
                model.AddConstr(ptot <= Capacity(p) * open(p), "Capacity" & p)
            Next

            ' Demand constraints
            For w As Integer = 0 To nWarehouses - 1
                Dim dtot As GRBLinExpr = 0
                For p As Integer = 0 To nPlants - 1
                    dtot.AddTerm(1.0, transport(w, p))
                Next
                model.AddConstr(dtot = Demand(w), "Demand" & w)
            Next

            ' Guess at the starting point: close the plant with the highest
            ' fixed costs; open all others

            ' First, open all plants
            For p As Integer = 0 To nPlants - 1
                open(p).Set(GRB.DoubleAttr.Start, 1.0)
            Next

            ' Now close the plant with the highest fixed cost
            Console.WriteLine("Initial guess:")
            Dim maxFixed As Double = -GRB.INFINITY
            For p As Integer = 0 To nPlants - 1
                If FixedCosts(p) > maxFixed Then
                    maxFixed = FixedCosts(p)
                End If
            Next
            For p As Integer = 0 To nPlants - 1
                If FixedCosts(p) = maxFixed Then
                    open(p).Set(GRB.DoubleAttr.Start, 0.0)
                    Console.WriteLine("Closing plant " & p & vbLf)
                    Exit For
                End If
            Next

            ' Use barrier to solve root relaxation
            model.GetEnv().Set(GRB.IntParam.Method, GRB.METHOD_BARRIER)

            ' Solve
            model.Optimize()

            ' Print solution
            Console.WriteLine(vbLf & "TOTAL COSTS: " & model.Get(GRB.DoubleAttr.ObjVal))
            Console.WriteLine("SOLUTION:")
            For p As Integer = 0 To nPlants - 1
                If open(p).Get(GRB.DoubleAttr.X) = 1.0 Then
                    Console.WriteLine("Plant " & p & " open:")
                    For w As Integer = 0 To nWarehouses - 1
                        If transport(w, p).Get(GRB.DoubleAttr.X) > 0.0001 Then
                            Console.WriteLine("  Transport " & _
                                              transport(w, p).Get(GRB.DoubleAttr.X) & _
                                              " units to warehouse " & w)
                        End If
                    Next
                Else
                    Console.WriteLine("Plant " & p & " closed!")
                End If

            Next

            ' Dispose of model and env
            model.Dispose()
            env.Dispose()

        Catch e As GRBException
            Console.WriteLine("Error code: " & e.ErrorCode & ". " & e.Message)
        End Try
    End Sub
End Class
