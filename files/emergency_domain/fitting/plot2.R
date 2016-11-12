library(MASS)

jan_calls <- 'C:/Users/ashwi/workspace/RDDLv2/files/emergency_domain/XYT.csv'
big_calls <- 'C:/Users/ashwi/workspace/RDDLv2/files/emergency_domain/XYT_Big.csv'
callFile <- big_calls

calls <- read.csv( callFile )
attach(calls)

Gap <- pmax(0.001,Gap)

get.formula <- function(n,k){
  rhs.str <- "Gap~Time"
  for (i in seq(n,k)){
    rhs.str <- paste( rhs.str, "+(Time>=",i-1,"&Time<",i,")",sep="")
  }
  return( as.formula(rhs.str) )
} 

gamma.model <- vector(mode="list",length=24)
gamma.model.shape <- vector(mode="list",length=24)
gamma.pred <- vector(mode="list",length=24)

for (k in seq(1,24)){
  this.form = get.formula(1,k)
  
  gamma.model[[k]] <- glm( this.form, family=Gamma(link="inverse") )
  gamma.model.shape[[k]] <- gamma.shape(gamma.model[[k]])
  summary(gamma.model[[k]],dispersion = 1/gamma.model.shape[[k]]$alpha)
  gamma.pred[[k]] <- predict(gamma.model[[k]],type="response",
                       se.fit=T,dispersion = 1/gamma.model.shape[[k]]$alpha)
  
  pdf( paste("model_gamma_f_",k,".pdf",sep="") )
  plot(Time,Gap,xlim = c(0,24), xlab = "Time",ylab = "Gap",pch=4,main=paste("1/E[Gap]~f(",k,") :", round(gamma.model[[k]]$aic,2),sep="") )
  points(tail(Time,-1),gamma.pred[[k]]$fit,col='blue',pch=20)
  
  points(tail(Time,-1),gamma.pred[[k]]$fit+3*gamma.pred[[k]]$se,col='red',pch=20)
  points(tail(Time,-1),gamma.pred[[k]]$fit-3*gamma.pred[[k]]$se,col='green',pch=20)
  
  legend( "topright", c("Mean","+3SD","-3SD"), fill=c("blue","red","green") )
  dev.off()
  # linear.model = glm( this.form,family=gaussian(link="identity") )
  # summary(linear.model)
  # linear.pred = predict( linear.model, type="response", se.fit=T )
  # 
  # plot(Time,Gap,xlim = c(0,24), xlab = "Time",ylab = "Gap",pch=4,main=paste("E[Gap]~f(",k,")",sep=""))
  # points(tail(Time,-1),linear.pred$fit,col='blue',pch=20)
  # 
  # points(tail(Time,-1),linear.pred$fit+3*linear.pred$se,col='red',pch=20)
  # points(tail(Time,-1),linear.pred$fit-3*linear.pred$se,col='green',pch=20)
  # 
  # legend( locator(1), c("Mean","+3SD","-3SD"), fill=c("blue","red","green") )
  pdf( paste("pred_f_",k,".pdf",sep="") ) 
  plot( Gap, main="Emergencies in January 2011")
  lines( gamma.pred[[k]]$fit, col="blue", lwd=2 )
  #lines( linear.pred$fit, col="green", lwd=2 )
  legend( "topright", c(paste("Gamma(",k,")",sep="")),fill=c("blue") )
  dev.off()
  
  pdf( paste("hist_f_",k,".pdf",sep="") )
  hist( gamma.pred[[k]]$fit, main=paste("Histogram for 1/E[Gap]~f(",k,")",sep=""), breaks=50, xlab="Predicted Gap" )
  dev.off()
  
  #hist( linear.pred$fit, main=paste("Histogram for E[Gap]~f(",k,")",sep=""), breaks=50 )
  #print( paste(this.form, round(gamma.model[[k]]$aic,2), round(gamma.model[[k]]$deviance,2), "(", gamma.model[[k]]$df.residual,")",sep="" ) )
}

outputs <- data.frame()
for ( k in seq(1,length(gamma.model)) ){
  outputs <- rbind(outputs, data.frame(k=k,aic=round(gamma.model[[k]]$aic,2),resid=round(gamma.model[[k]]$deviance,2), df=gamma.model[[k]]$df.residual ))
}

print(outputs)

norm.it <- function(x){
  return( (x-min(x))/(max(x)-min(x)) )
}

pdf( "model_selection.pdf" )

plot( x=seq(1,24), y=norm.it(outputs$aic), main="Model Slection vs k",ylab="Scaled Metric", 
      xlab="k",col="blue", type = 'l', ylim=c(0,1), xlim=c(0,24) )
lines( norm.it(outputs$resid), main="Model Selection vs k", col="red")
legend("top", c("AIC","Resid. Deviance"), fill=c("blue","red") )
dev.off()