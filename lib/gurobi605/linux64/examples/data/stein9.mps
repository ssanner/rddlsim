*NAME:         stein9
*ROWS:         13
*COLUMNS:      9
*INTEGER:      9
*NONZERO:      45
*BEST SOLN:    5 (opt)
*LP SOLN:      4.0
*SOURCE:       George L. Nemhauser (Georgia Institute of Technology)
*              John W. Gregory (Cray Research)
*              E. Andrew Boyd (Rice University)                        
*APPLICATION:  unknown
*COMMENTS:     pure 0/1 IP
*      
*      
NAME          STEIN9 
ROWS
 N  OBJ     
 G  A1      
 G  A2      
 G  A3      
 G  A4      
 G  A5      
 G  A6      
 G  A7      
 G  A8      
 G  A9      
 G  A10     
 G  A11     
 G  A12     
 G  OB2     
COLUMNS
    MARK0000  'MARKER'                 'INTORG'
    0001      OBJ                  1   A2                   1
    0001      A3                   1   A7                   1
    0001      A10                  1   OB2                  1
    0002      OBJ                  1   A1                   1
    0002      A3                   1   A8                   1
    0002      A11                  1   OB2                  1
    0003      OBJ                  1   A1                   1
    0003      A2                   1   A9                   1
    0003      A12                  1   OB2                  1
    0004      OBJ                  1   A1                   1
    0004      A5                   1   A6                   1
    0004      A10                  1   OB2                  1
    0005      OBJ                  1   A2                   1
    0005      A4                   1   A6                   1
    0005      A11                  1   OB2                  1
    0006      OBJ                  1   A3                   1
    0006      A4                   1   A5                   1
    0006      A12                  1   OB2                  1
    0007      OBJ                  1   A4                   1
    0007      A8                   1   A9                   1
    0007      A10                  1   OB2                  1
    0008      OBJ                  1   A5                   1
    0008      A7                   1   A9                   1
    0008      A11                  1   OB2                  1
    0009      OBJ                  1   A6                   1
    0009      A7                   1   A8                   1
    0009      A12                  1   OB2                  1
    MARK0001  'MARKER'                 'INTEND'
RHS
    RHS       A1                   1   A2                   1
    RHS       A3                   1   A4                   1
    RHS       A5                   1   A6                   1
    RHS       A7                   1   A8                   1
    RHS       A9                   1   A10                  1
    RHS       A11                  1   A12                  1
    RHS       OB2                  4
BOUNDS
 UP bnd       0001                 1
 UP bnd       0002                 1
 UP bnd       0003                 1
 UP bnd       0004                 1
 UP bnd       0005                 1
 UP bnd       0006                 1
 UP bnd       0007                 1
 UP bnd       0008                 1
 UP bnd       0009                 1
ENDATA
