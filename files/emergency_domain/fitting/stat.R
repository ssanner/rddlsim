library(MASS)

jan_calls <- 'C:/Users/ashwi/workspace/RDDLv2/files/emergency_domain/XYT.csv'
big_calls <- 'C:/Users/ashwi/workspace/RDDLv2/files/emergency_domain/XYT_Big.csv'
callFile <- jan_calls

calls <- read.csv( callFile )
attach(calls)

Gap <- pmax(0.001, Gap)

hist(Gap, main = "Histogram of Interarrival Time",breaks=100,xlim=c(0,24))

op_table <- ""

for ( lhs in c( "Gap", "I(1/Gap)","log(Gap)" ) ){
  for( rhs in c("Time","I(1/Time)","log(Time)" ) ){
    for (fmly in c("Gamma","gaussian")){
      for (lnk in c("inverse","inverse","log")){
      
        if (fmly == "Gamma" & lnk == "identity" ){
          next
        }
    
        if (lnk == "identity"){
          lnk_str <- ""
        }else if (lnk == "inverse"){
          lnk_str <- "1/"
        }else if (lnk == "log" ){
          lnk_str <- "log"
        }
        
        if (fmly == "Gamma"){
          fmly_str <- "G"
        }else if (fmly == "gaussian"){
          fmly_str <- "N"
        }
                
        tryCatch( { 
          this_model <- glm( as.formula( paste(lhs, "~", rhs) ), family = get(fmly)(link=lnk) )
          label_str <- paste(lnk_str,"E[",lhs,"]~",#fmly_str,"(",
                             rhs, sep= "" )
          print(label_str)
          
          if ( fmly == "Gamma" ){
            this_shape <- gamma.shape(this_model)
            print( summary( this_model, dispersion=1/this_shape$alpha ) )
            this_pred <- predict( this_model, type="response", se.fit = T, dispersion = 1/this_shape$alpha)
          } else {
            print( summary( this_model ) )
            this_pred <- predict( this_model, type="response", se.fit = T)
          }
          
          plot(Time,Gap,xlim = c(0,24),xlab = "Time",ylab = "Gap",pch=4,main=label_str)
          points(tail(Time,-1),this_pred$fit,col='blue',pch=20)
          
          points(tail(Time,-1),this_pred$fit+2*this_pred$se,col='red',pch=20)
          points(tail(Time,-1),this_pred$fit-2*this_pred$se,col='green',pch=20)
          
          op_table = rbind( op_table, 
                            paste(lnk_str,"E[",lhs,"]~",fmly_str,"(",rhs,") ", 
                                  round(this_model$aic,2), " ", round(this_model$deviance,2), "(", this_model$df.residual, ") ", 
                                  round(this_model$null.deviance,2), "(", this_model$df.null, ")", sep="" ) )
        }, error = function(e) { print(e) } )
        
        print( op_table )
        # legend( locator(1), c(label_str), col=c("blue") )
        # 
        # plot(Gap,ylab="Gap",main=label_str)
        # lines(this_pred$fit,col='blue')
        # legend( locator(1), c(label_str),col=c("blue"))
        
        
        
      }
    }
  }
}

# 
# #http://civil.colorado.edu/~balajir/CVEN6833/lectures/GammaGLM-01.pdf
# #µi = bo + b1/xi
# glm_7_1 <- glm( Gap ~ I(1/Time), family=Gamma(link="identity") )
# shape_7_1 <- gamma.shape(glm_7_1)
# summary(glm_7_1,dispersion=1/shape_7_1$alpha)
# pred_7_1 <- predict(glm_7_1,type="response",dispersion=1/shape_7_1$alpha,se=T)
# 
# #
# lm_7_2 <- glm( Gap ~ I(1/Time) )
# summary(lm_7_2)
# pred_7_2 <- predict( lm_7_2, type="response",se=T)
# 
# #Michaelson-Morley model
# #1/µi = b0 + b1/xi
# glm_8_1 <- glm( Gap ~ I(1/Time), family=Gamma(link="inverse") )
# shape_8_1 <- gamma.shape(glm_8_1)
# summary(glm_8_1,dispersion=1/shape_8_1$alpha)
# pred_8_1 <- predict( glm_8_1, type="response", se=T)
# 
# #1/yi = b0 + b1/xi + ei
# lm_8_2 <- lm(I(1/Gap)~I(1/Time))
# pred_8_2 <- predict(lm_8_2,type="response", se=T)
# summary(lm_8_2)
# 
# #1/µi = b0 + b1/xi
# lm_8_3 <- glm(Gap~I(1/Time),family=gaussian(link="inverse"))
# summary(lm_8_3)
# pred_8_3 <- predict(lm_8_3,type="response", se=T)
# 
# #glm mine 1
# glm_9_1 <- glm( Gap ~ Time, family=Gamma(link="inverse") )
# shape_9_1 <- gamma.shape(glm_9_1)
# summary(glm_9_1,dispersion=1/shape_9_1$alpha)
# pred_9_1 <- predict( glm_9_1, type="response", se=T)
# 
# lm_9_2 <- glm( Gap ~ Time, family=gaussian(link="inverse") )
# summary(lm_9_2)
# pred_9_2 <- predict( glm_9_2, type="response", se=T)
# 
# lm_9_3 <- glm( Gap ~ Time, family=gaussian(link="identity") )
# summary(lm_9_3)
# pred_9_3 <- predict( lm_9_3, type="response", se=T)
# 
# #glm mine 2
# # glm_9_2 <- glm( Gap ~ Time, family=Gamma(link="identity") )
# # shape_9_2 <- gamma.shape(glm_9_2)
# # summary(glm_9_2,dispersion=1/shape_9_2$alpha)
# # pred_9_2 <- predict( glm_9_2, type="response", se=T)
# 
# 
# 
# plot(Time,Gap,xlim = c(0,24),main = "Time vs Gap",xlab = "Time",ylab = "Gap",pch=4)
# points(tail(Time,-1),glm_7_1$fit,col='red',pch=20)
# points(tail(Time,-1),lm_7_2$fit,col='green',pch=20)
# points(tail(Time,-1),glm_8_1$fit,col='blue',pch=20)
# points(tail(Time,-1),lm_8_2$fit,col='violet',pch=20)
# points(tail(Time,-1),lm_8_3$fit,col='orange',pch=20)
# points(tail(Time,-1),glm_9_1$fit,col='pink',pch=20)
# legend( locator(1), c("GLM_7_1","LM_7_2","GLM_8_1","LM_8_2","LM_8_3","GLM_9_1"), 
#                     col=c("red","green","blue","violet","orange","pink") )
# 
# plot(Gap,ylab="Gap")
# lines(glm_7_1$fit,col='blue')
# lines(glm_9_1$fit,col='red')
# legend( locator(1), c("GLM_7_1","GLM_9_1"),col=c("blue","red"))
# 
# # c( paste("GLM_7_1(",round(glm_7_1$aic,2),")",collapse = ""), 
# #    paste("LM_7_2(", round(lm_7_2$aic,2),")",collapse = ""),
# #    paste("LM_7_2(", round(glm_8_1$aic,2),")",collapse = ""),
# #    paste("LM_7_2(", round(lm_8_2$aic,2),")",collapse = ""),
# #    paste("LM_7_2(", round(lm_8_3$aic,2),")",collapse = "") ), 
# # 
# # glm_form <- Gap~Time#+(calls$Time<5)+(calls$Time>18)*calls$Time#+(calls$Time>18)*calls$Time
# # linear_form <- Gap~I(1/Time)#+(calls$Time<5)+(calls$Time>18)*calls$Time
# # 
# # linearModel <- glm(linear_form)
# # summary(linearModel)
# # linearPred <- predict(linearModel,type="response", se.fit = T)
# # 
# # gammaModel <- glm(glm_form,family=Gamma(link="inverse"))
# # gammaShape <- gamma.shape(gammaModel)
# # 
# # summary(gammaModel,dispersion =1.0/gammaShape$alpha)
# # gammaPred <- predict(gammaModel, type="response",dispersion = 1.0/gammaShape$alpha, se.fit=T)
# # 
# # #hist(rgamma(n=1000,shape = gammaShape, gammaModel$coefficients))
# # plot(Time,Gap,xlim = c(0,24),main = "Time vs Gap",xlab = "Time",ylab = "Gap",pch=4)
# # points(tail(Time,-1),linearPred$fit,col='red')
# # points(tail(Time,-1),gammaPred$fit,col='blue',pch=20)
# # legend( locator(1), c( paste("Linear(",round(linearModel$aic,2),")",collapse = ""), paste("GLM-Gamma(", round(gammaModel$aic,2),")",collapse = "")), lty=c(1,2), lwd=c(1,1),col=c("red","blue") )
# # 
# # plot(Time,calls$Gap,xlim = c(0,24),main = "Linear Model",xlab = "Time",ylab = "Gap",pch=4)
# # points(tail(Time,-1),linearPred$fit,col='blue',lty=1)
# # points(tail(Time,-1),linearPred$fit+4*linearPred$se,col='green',cex=0.5)
# # points(tail(Time,-1),linearPred$fit-4*linearPred$se,col='red',cex=0.5)
# # legend( locator(1), c("Mean","+4SD","-4SD"), lty=c(1,1,1), lwd=c(1,1,1), col=c("blue","green","red"))
# # 
# # plot(Time,Gap,xlim = c(0,24), main="GLM-Gamma",xlab="Time",ylab="Gap")
# # points(tail(Time,-1),gammaPred$fit,col='blue')
# # points(tail(Time,-1), gammaPred$fit+4*gammaPred$se, col='green',cex=0.5)
# # points(tail(Time,-1), gammaPred$fit-4*gammaPred$se, col='red',cex=0.5)
# # legend( locator(1), c("Mean","+4SD","-4SD"), lty=c(1,1,1), lwd=c(1,1,1), col=c("blue","green","red"))
# # 
# # plot(Gap,ylab="Gap")
# # lines(linearModel$fit,col='blue',lty=2,lwd=2)
# # lines(gammaPred$fit,col='red',lty=3,lwd=2)
# # legend( locator(1), c("Linear Model","GLM-Gamma"),lty=c(2,3),lwd=c(2,2),col=c("blue","red"))
# # 
# # hist(gammaPred$fit,breaks=50)
# # hist(linearPred$fit,breaks=50)
# # 
# # sampled_t = seq(0,23.9,0.1)
# # means_t = predict( gammaModel, type="response", dispersion=1.0/gammaShape$alpha, 
# #          newdata=data.frame(Time=sampled_t, Gap="b", X=mean(X), Y=mean(Y) ) )
# # plot(sampled_t,means_t)
