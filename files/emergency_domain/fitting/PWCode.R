jan_calls <- 'C:/Users/ashwi/workspace/RDDLv2/files/emergency_domain/XYT.csv'
big_calls <- 'C:/Users/ashwi/workspace/RDDLv2/files/emergency_domain/XYT_Big.csv'
callFile <- jan_calls

calls <- read.csv( callFile )
attach(calls)

X.nz <- X[X!=0]
Y.nz <- Y[X!=0]
Time.nz <- Time[X!=0]
Code.nz <- relevel(Code[X!=0],ref="Code3Med")
gap <- Time.nz[2:length(Time.nz)]-Time.nz[1:length(Time.nz)-1]
gap[gap<0] <- 24+gap[gap<0]
gap <- pmax(0.001,gap)

stopifnot((length(X.nz)==length(Y.nz))&(length(X.nz)==length(Time.nz))&
            (length(X.nz)==length(Code.nz)))

all.data <- data.frame(r=Code.nz,x=X.nz,y=Y.nz,t=Time.nz,g=c(gap,NA))
print( tail(all.data) )

split.index <- floor(length(X.nz)/2)
train.data <- all.data[1:split.index,]
print( table(train.data[,'r'] ) )
print( tail(train.data) )

test.data <- all.data[1+split.index:dim(all.data)[1]-1,]
print( table(test.data[,'r'] ) )
print( tail(test.data) )

require(earth)
mr <- earth(r~x+y+t,degree=3,nk=30, pmethod="backward",
            data=train.data,trace=3,thresh=0)
summary(mr)

#training error
# predictions.g <- predict( mg$glm.list[[1]],se.fit = T,
#                           type="response",dispersion = 1/myshape$alpha)
# emp.density <- density(train.data['t'][,1],n=100)
# plot(emp.density$x, emp.density$y, type='l',col='black',ylab='Density',
#      xlab='Time',xlim=c(0,24),main='Arrival of Emergencies (2004-2010)')
# 
# pred.t <- train.data['t'][,1]+predictions.g$fit
# pred.t[pred.t>=24] <- pred.t[pred.t>=24]-24
# pred.density <- density(pred.t,n=100)
# lines(pred.density$x,pred.density$y,col='blue')
# 
# pred.t.upper <- train.data['t'][,1]+predictions.g$fit+4*predictions.g$se.fit
# pred.t.upper[pred.t.upper>=24] <- pred.t.upper[pred.t.upper>=24]-24
# pred.density.upper <- density(pred.t.upper,n=100)
# lines(pred.density.upper$x,pred.density.upper$y,col='green')
# 
# pred.t.lower <- train.data['t'][,1]+predictions.g$fit-4*predictions.g$se.fit
# pred.t.lower[pred.t.lower>=24] <- pred.t.lower[pred.t.lower>=24]-24
# pred.density.lower <- density(pred.t.lower,n=100)
# lines(pred.density.lower$x,pred.density.lower$y,col='red')
# 
# legend('bottom',legend=c('Observed','Predicted','+4SD','-4SD'),cex=0.8,
#        fill=c('black','blue','green','red'))
# 
# train.err <- pred.t[1:length(pred.t)-1]-train.data['t'][,1][2:dim(train.data)[1]]
# print( sum(train.err*train.err) )
# 
# #test error
# predictions.test.g <- predict( mg,newdata = test.data,type="response")
# emp.density <- density(test.data['t'][,1],n=100)
# plot(emp.density$x, emp.density$y, type='l',col='black',ylab='Density',
#      xlab='Time',xlim=c(0,24),main='Arrival of Emergencies (2004-2010)')
# 
# pred.test.t <- test.data['t'][,1]+predictions.test.g
# pred.test.t[pred.test.t>=24] <- pred.test.t[pred.test.t>=24]-24
# pred.test.density <- density(pred.test.t,n=100)
# lines(pred.test.density$x,pred.test.density$y,col='blue')
# 
# legend('bottom',legend=c('Observed','Predicted'),cex=0.8,
#        fill=c('black','blue'))
# 
# test.err <- pred.test.t[1:length(pred.test.t)-1]-test.data['t'][,1][2:dim(test.data)[1]]
# print( sum(test.err*test.err) )


