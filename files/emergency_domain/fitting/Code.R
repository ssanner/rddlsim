require(foreign)
require(nnet)

two.fold.cv <- function(  ){
  jan_calls <- 'C:/Users/ashwi/workspace/RDDLv2/files/emergency_domain/XYT.csv'
  big_calls <- 'C:/Users/ashwi/workspace/RDDLv2/files/emergency_domain/XYT_Big.csv'
  callFile <- big_calls
  
  calls <- read.csv( callFile )
  attach(calls)
  
  X.nz <- X[X!=0]
  Y.nz <- Y[X!=0]
  Time.nz <- Time[X!=0]
  Code.nz <- relevel(Code[X!=0],ref="Code3Med")

  stopifnot((length(X.nz)==length(Y.nz))&(length(X.nz)==length(Time.nz))&
              (length(X.nz)==length(Code.nz)))
  
  all.data <- data.frame(r=Code.nz,x=X.nz,y=Y.nz,t=Time.nz)
  print( tail(all.data) )
  
  split.index <- floor(length(X.nz)/2)
  train.data <- all.data[1:split.index,]
  print( table(train.data[,'r'] ) )
  print( tail(train.data) )
  
  test.data <- all.data[1+split.index:dim(all.data)[1]-1,]
  print( table(test.data[,'r'] ) )
  print( tail(test.data) )
  
  mmm<-multinom(r~x+y+t,data = train.data)
  print( summary(mmm) )
  z<-summary(mmm)$coefficients/summary(mmm)$standard.errors
  p <- (1 - pnorm(abs(z), 0, 1))*2
  print( p )
  
  p.mmm <- predict( mmm,type="probs",se=TRUE)
  # print( head(p.mmm) )
  plot(1:length(p.mmm[,1]),p.mmm[,1],type='l',col=1,ylim=c(0,1))
  for( c in 1:dim(p.mmm)[2] ){
    lines(1:length(p.mmm[,c]),p.mmm[,c],col=c,pch=c)
  }
  legend("topright",fill=1:dim(p.mmm)[2],legend=names(p.mmm[1,]),pch=1:dim(p.mmm)[2],cex=0.5)
  
  print( tail( predict( mmm, type="probs" ) ) ) 
  print( tail( predict( mmm, type="probs", newdata=test.data) ) )
  
  #training error
  r.mmm <- predict(mmm, type="class")
  print( table(r.mmm) )
  print( paste( 'Training error', sum(r.mmm!=train.data[,'r'])/dim(train.data)[1] ) )
  
  
  #test error
  test.mmm <- predict(mmm, type="class", newdata = test.data )
  print( table(test.mmm) )
  print( paste( 'Test error', sum(test.mmm!=test.data[,'r'])/dim(test.data)[1] ) )
  return( mmm )
}

calc.entropy <- function(response.vector){
  ttt<-table(response.vector)
  p.ttt<-ttt/sum(ttt)
  return( -sum(p.ttt*log(p.ttt) ) )
}

mr <- two.fold.cv()
