' Copyright 2014, Gurobi Optimization, Inc.

' Solve the classic diet model, showing how to add constraints
' to an existing model.

Imports System
Imports Gurobi

Class diet_vb
    Shared Sub Main()
        Try

            ' Nutrition guidelines, based on
            ' USDA Dietary Guidelines for Americans, 2005
            ' http://www.health.gov/DietaryGuidelines/dga2005/
            Dim Categories As String() = New String() {"calories", "protein", "fat", _
                                                       "sodium"}
            Dim nCategories As Integer = Categories.Length
            Dim minNutrition As Double() = New Double() {1800, 91, 0, 0}
            Dim maxNutrition As Double() = New Double() {2200, GRB.INFINITY, 65, 1779}

            ' Set of foods
            Dim Foods As String() = New String() {"hamburger", "chicken", "hot dog", _
                                                  "fries", "macaroni", "pizza", _
                                                  "salad", "milk", "ice cream"}
            Dim nFoods As Integer = Foods.Length
            Dim cost As Double() = New Double() {2.49, 2.89, 1.5R, 1.89, 2.09, 1.99, _
                                                 2.49, 0.89, 1.59}

            ' Nutrition values for the foods
            ' hamburger
            ' chicken
            ' hot dog
            ' fries
            ' macaroni
            ' pizza
            ' salad
            ' milk
            ' ice cream
            Dim nutritionValues As Double(,) = New Double(,) {{410, 24, 26, 730}, _
                                                              {420, 32, 10, 1190}, _
                                                              {560, 20, 32, 1800}, _
                                                              {380, 4, 19, 270}, _
                                                              {320, 12, 10, 930}, _
                                                              {320, 15, 12, 820}, _
                                                              {320, 31, 12, 1230}, _
                                                              {100, 8, 2.5, 125}, _
                                                              {330, 8, 10, 180}}

            ' Model
            Dim env As New GRBEnv()
            Dim model As New GRBModel(env)
            model.Set(GRB.StringAttr.ModelName, "diet")

            ' Create decision variables for the nutrition information,
            ' which we limit via bounds
            Dim nutrition As GRBVar() = New GRBVar(nCategories - 1) {}
            For i As Integer = 0 To nCategories - 1
                nutrition(i) = model.AddVar(minNutrition(i), maxNutrition(i), 0, _
                                            GRB.CONTINUOUS, Categories(i))
            Next

            ' Create decision variables for the foods to buy
            Dim buy As GRBVar() = New GRBVar(nFoods - 1) {}
            For j As Integer = 0 To nFoods - 1
                buy(j) = model.AddVar(0, GRB.INFINITY, cost(j), GRB.CONTINUOUS, _
                                      Foods(j))
            Next

            ' The objective is to minimize the costs
            model.Set(GRB.IntAttr.ModelSense, 1)

            ' Update model to integrate new variables
            model.Update()

            ' Nutrition constraints
            For i As Integer = 0 To nCategories - 1
                Dim ntot As GRBLinExpr = 0
                For j As Integer = 0 To nFoods - 1
                    ntot.AddTerm(nutritionValues(j, i), buy(j))
                Next
                model.AddConstr(ntot = nutrition(i), Categories(i))
            Next

            ' Solve
            model.Optimize()
            PrintSolution(model, buy, nutrition)

            Console.WriteLine(vbLf & "Adding constraint: at most 6 servings of dairy")
            model.AddConstr(buy(7) + buy(8) <= 6, "limit_dairy")

            ' Solve
            model.Optimize()

            PrintSolution(model, buy, nutrition)

            ' Dispose of model and env
            model.Dispose()
            env.Dispose()

        Catch e As GRBException
            Console.WriteLine("Error code: " & e.ErrorCode & ". " & e.Message)
        End Try
    End Sub

    Private Shared Sub PrintSolution(ByVal model As GRBModel, ByVal buy As GRBVar(), _
                                     ByVal nutrition As GRBVar())
        If model.Get(GRB.IntAttr.Status) = GRB.Status.OPTIMAL Then
            Console.WriteLine(vbLf & "Cost: " & model.Get(GRB.DoubleAttr.ObjVal))
            Console.WriteLine(vbLf & "Buy:")
            For j As Integer = 0 To buy.Length - 1
                If buy(j).Get(GRB.DoubleAttr.X) > 0.0001 Then
                    Console.WriteLine(buy(j).Get(GRB.StringAttr.VarName) & " " & _
                                      buy(j).Get(GRB.DoubleAttr.X))
                End If
            Next
            Console.WriteLine(vbLf & "Nutrition:")
            For i As Integer = 0 To nutrition.Length - 1
                Console.WriteLine(nutrition(i).Get(GRB.StringAttr.VarName) & " " & _
                                  nutrition(i).Get(GRB.DoubleAttr.X))
            Next
        Else
            Console.WriteLine("No solution")
        End If
    End Sub
End Class
