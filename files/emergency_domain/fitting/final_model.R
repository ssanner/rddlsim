library(MASS)

jan_calls <- 'C:/Users/ashwi/workspace/RDDLv2/files/emergency_domain/XYT.csv'
big_calls <- 'C:/Users/ashwi/workspace/RDDLv2/files/emergency_domain/XYT_Big.csv'
callFile <- big_calls

calls <- read.csv( callFile )
attach(calls)

Gap <- pmax(0.001,Gap)

final.form <- Gap~Time+(Time>=0&Time<1)+(Time>=1&Time<2)+(Time>=2&Time<3)+(Time>=3&Time<4)+
  #(Time>=4&Time<5)+
  (Time>=5&Time<6)+(Time>=6&Time<7)+(Time>=7&Time<8)+
  (Time>=8&Time<9)+(Time>=9&Time<10)+(Time>=10&Time<11)+#(Time>=11&Time<12)+
  (Time>=12&Time<13)+#(Time>=13&Time<14)+
  (Time>=14&Time<15)+(Time>=15&Time<16)+
  (Time>=16&Time<17)+(Time>=17&Time<18)+(Time>=18&Time<19)+(Time>=19&Time<20)+
  (Time>=20&Time<21)+#(Time>=21&Time<22)+
  (Time>=22&Time<23)+(Time>=23&Time<24)
  
final.model <- glm(final.form, family=Gamma(link="inverse"))
final.model.shape <- gamma.shape(final.model)
final.model.pred <- predict(final.model, se.fit=T, type="response",dispersion=1/final.model.shape$alpha)
summary(final.model,dispersion = 1/final.model.shape$alpha)
anova(final.model,dispersion = 1/final.model.shape$alpha, test="Chisq")

pdf("final_model_gamma.pdf")
plot(Time,Gap,xlim = c(0,24), xlab = "Time",ylab = "Gap",pch=4,main= "Piecewise Gamma" )
points(tail(Time,-1),final.model.pred$fit,col='blue',pch=20)

points(tail(Time,-1),final.model.pred$fit+10*final.model.pred$se,col='red',pch=20)
points(tail(Time,-1),final.model.pred$fit-10*final.model.pred$se,col='green',pch=20)

legend( "topright", c("Mean","+10SD","-10SD"), fill=c("blue","red","green") )
dev.off()

pdf( "final_pred.pdf" ) 
plot( Gap, main="Emergencies in January 2011")
lines( final.model.pred$fit, col="blue", lwd=2 )
dev.off()

pdf( "final_hist.pdf" )
hist( final.model.pred$fit, main=paste("",sep=""), breaks=50, xlab="Predicted Gap" )
dev.off()

