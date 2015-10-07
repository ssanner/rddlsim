function diet()
% diet Solve the classic diet model

% Copyright 2014, Gurobi Optimization, Inc


% Nutrition guidelines, based on
% USDA Dietary Guidelines for Americans, 2005
% http://www.health.gov/DietaryGuidelines/dga2005/

ncategories = 4;
categories = {'calories'; 'protein'; 'fat'; 'sodium'};
%             minNutrition maxNutrition
categorynutrition = [ 1800 2200;   % calories
                      91   inf;    % protein
                      0    65;     % fat
                      0    1779];  % sodium

nfoods = 9;
foods = {'hamburger';
         'chicken';
         'hot dog';
         'fries';
         'macaroni';
         'pizza';
         'salad';
         'milk';
         'ice cream'};

foodcost = [2.49;  % hamburger
            2.89;  % chicken
            1.50;  % hot dog
            1.89;  % fries
            2.09;  % macaroni
            1.99;  % pizza
            2.49;  % salad
            0.89;  % milk
            1.59]; % ice cream

                  % calories protein fat sodium
nutritionValues = [ 410      24      26  730;   % hamburger
                    420      32      10  1190;  % chicken
                    560      20      32  1800;  % hot dog
                    380      4       19  270;   % fries
                    320      12      10  930;   % macaroni
                    320      15      12  820;   % pizza
                    320      31      12  1230;  % salad
                    100      8       2.5 125;   % milk
                    330      8       10  180];  % ice cream
nutritionValues = sparse(nutritionValues);
model.modelName = 'diet';

% The variables are layed out as [ buy; nutrition]
model.obj   = [ foodcost;         zeros(ncategories, 1)];
model.lb    = [ zeros(nfoods, 1); categorynutrition(:, 1)];
model.ub    = [  inf(nfoods, 1);  categorynutrition(:, 2)];
model.A     = [ nutritionValues' -speye(ncategories)];
model.rhs   = zeros(ncategories, 1);
model.sense = repmat('=', ncategories, 1);


function printSolution(result)
    if strcmp(result.status, 'OPTIMAL')
        buy       = result.x(1:nfoods);
        nutrition = result.x(nfoods+1:nfoods+ncategories);
        fprintf('\nCost: %f\n', result.objval);
        fprintf('\nBuy:\n')
        for f=1:nfoods
            if buy(f) > 0.0001
                fprintf('%10s %g\n', foods{f}, buy(f));
            end
        end
        fprintf('\nNutrition:\n')
        for c=1:ncategories
            fprintf('%10s %g\n', categories{c}, nutrition(c));
        end
    else
        fprintf('No solution\n');
    end
end

% Solve
results = gurobi(model);
printSolution(results);

fprintf('\nAdding constraint at most 6 servings of dairy\n')
milk = find(strcmp('milk', foods));
icecream = find(strcmp('ice cream', foods));
model.A(end+1,:) = sparse([1; 1], [milk; icecream], 1, ...
        1, nfoods + ncategories);
model.rhs(end+1) = 6;
model.sense(end+1) = '<';

% Solve
results = gurobi(model);
printSolution(results)

end
