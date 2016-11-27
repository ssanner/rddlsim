library(MASS)
library(mixtools)
library(car)
library(plotrix)

get.formula <- function(n,k,u){
  form <- Time[X!=0]
  for (i in seq(n,k,u)){
    form <- cbind(form,Time[X!=0]>=i-1&Time[X!=0]<i)
  }
  return( form )
} 

# sample.mixture <- function(modx,mody,tm){
#   probs <- modx$lambda%*%t(mody$lambda)
#   components <- sample( 1:length(tm), prob=modl$lambda, size=length(tm), replace=TRUE )
#   # print( length(components) )
#   thetas.x<-modx$beta[,components]#2 X |tm| 
#   mus <- colSums(rbind(1,tm)*thetas)
#   samples <- rnorm(n=length(tm),mean=mus,sd=modl$sigma[components])
# }


sample.mixture.mx <- function(modl,tm,arbvar=TRUE){
  components <- sample( 1:length(modl$lambda), prob=modl$lambda, size=length(tm), replace=TRUE )
  # print( length(components) )
  thetas<-modl$beta[,components]#2 X |tm| 
  # print( dim(thetas) )
  mus <- colSums(rbind(1,tm)*thetas)#2X|tm| * 2X|tm| = 2X|tm|
  # print( head(mus) )
  # print( head(modl$sigma[components]) )#|tm|X1
  sds<-1
  if( arbvar==TRUE){
    sds=modl$sigma[components]
  }else{
    sds=modl$sigma[rep(1,length(tm))]    
  }
  
  samples <- rnorm(n=length(tm),mean=mus,sd=sds)
  return( samples )  
}

sample.mixture.my <- function(modl,xs,tm,arbvar=TRUE){
  components <- sample( 1:length(modl$lambda), prob=modl$lambda, size=length(tm), replace=TRUE )
  # print( length(components) )
  thetas<-modl$beta[,components]#3X|tm|
  mus<-colSums(rbind(1,tm,xs)*thetas)
  # print( length(mus) )
  # print( length(modl$sigma[components]) )
  sds<-1
  if( arbvar==TRUE){
    sds=modl$sigma[components]
  }else{
    sds=modl$sigma[rep(1,length(tm))]    
  }
  
  samples <- rnorm(n=length(tm),mean=mus,sd=sds)
  return( samples )  
}

draw.lines <- function(mody){
  par(xpd=FALSE)
  plot(X.nz,Y.nz,main='Location of Emergencies in 2004-2010',xlab="X",ylab="Y")
  for( ycomp in 1:length(mody$lambda) ){
      #X-y relationship
      abline(a=mody$beta[1,ycomp],b=mody$beta[3,ycomp],col=ycomp)
  }
  par(xpd=TRUE)
  legend("bottom",legend = round(mody$lambda,2),
         xpd=TRUE, horiz=TRUE,inset=c(0,-0.35),bty="n",fill=1:length(mody$lambda), cex=0.6)
  
}

reconstruction <- function(modx,mody){
  par(xpd=FALSE)
  plot(X.nz,Y.nz,main='Location of Emergencies in 2004-2010',xlab="X",ylab="Y")
  for( xcomp in 1:length(modx$lambda) ){
    xs <- cbind(1,0)%*%modx$beta[,xcomp]
    for( ycomp in 1:length(mody$lambda) ){
      ys <- cbind(1,0,xs)%*%mody$beta[,ycomp]  
      points( xs, ys,col=c(xcomp,ycomp), pch='x')
      #X-y relationship
      abline(a=mody$beta[1,ycomp],b=mody$beta[3,ycomp],
               col=xcomp+ycomp)
      # print( mody$beta[1,ycomp]+mody$beta[2,ycomp]*12 )
      # print( mody$beta[3] )
    }
  }
  par(xpd=TRUE)
  legend("bottom",legend = rbind(round(modx$lambda,2),round(mody$lambda,2)),
         xpd=TRUE, horiz=TRUE,inset=c(0,-0.35),bty="n",fill=1:length(modx$lambda), cex=0.7)
   
  par(xpd=FALSE)
  plot(X.nz,Y.nz,main='Location of Emergencies in 2004-2010',xlab="X",ylab="Y")
  for( xcomp in 1:length(modx$lambda) ){
    xs <- cbind(1,0)%*%modx$beta[,xcomp]
    for( ycomp in 1:length(mody$lambda) ){
      ys <- cbind(1,0,xs)%*%mody$beta[,ycomp]  
      points( xs, ys,col=xcomp+ycomp, pch='x')
      segments(mean(xs)-3*modx$sigma[xcomp],mean(ys),mean(xs)+3*modx$sigma[xcomp],mean(ys), col = xcomp )
      segments(mean(xs),mean(ys)-3*mody$sigma[ycomp],mean(xs),mean(ys)+3*mody$sigma[ycomp], col = ycomp)
    }
  }
  par(xpd=TRUE)
  legend("bottom",legend = rbind(round(modx$lambda,2),round(mody$lambda,2) ),
         xpd=TRUE,horiz=TRUE,inset=c(0,-0.35),bty="n",fill=1:length(modx$lambda), cex=0.7)
  
}

# plot(X.nz,Y.nz,main='Location of Emergencies in 2004-2010')
# for( compo in 1:length(modx$lambda) ){
#   xs <- cbind(1,seq(0,24,0.01))%*%modx$beta[,compo]
#   ys <- cbind(1,seq(0,24,0.01),xs)%*%mody$beta[,compo]
#   
#   bivn.kde <- kde2d(xs,ys,n=100)
#   
#   image(bivn.kde,main=paste('Countour',compo))
#   contour(bivn.kde,add=T)
# }

# Code.nz <- Code[X!=0]



               # beta = rbind( 1,seq(1390,1425,7) ) )
               #beta = rbind( seq(1400,1420,5), 0 ),
               # lambda = c(0.0159445, 3.95773e-01, 3.45676e-01,  3.32448e-02,  2.09362e-01),
               # sigma = c(3.6069498, 1.33023e+00, 5.81191e-01,  9.59391e-03,  3.04548e+00),
               # beta = rbind( c(1401.3427927, 1.41646e+03, 1.41704e+03,  1.41762e+03,  1.41485e+03),
               #               c(-0.0014207, 5.50639e-03, 1.55210e-03, -7.73423e-05, -5.33047e-03) ) ,
               # maxit = 1 )

# mx <- regmixEM(X.nz,Time.nz,verb = T, beta = rbind( seq(1390,1425,7), 0 ) )



#centers are mx$beta
#predictor for P(Y|X) is not only X but the corresponding X centers from each component
# X.centers <- cbind(1,Time.nz)%*%mx$betac
# X.dist <- abs(X.centers-X.nz)
# closest.centers.idx <- apply(X.dist,1,which.min)
# closest.centers <- X.centers[cbind(seq_along(closest.centers.idx),closest.centers.idx)]



# my <- list()
# for( xcomp in 1:length(mx$lambda) ){
#   X.fit <- cbind(1,Time.nz)%*%mx$beta[,xcomp]
#   # X.diff <- X.nz-X.fit
#   my[[xcomp]] <- regmixEM(Y.nz,cbind(Time.nz,X.fit),verb=T,maxit=10,k=num.comp)
#                  # beta=rbind(mx$beta,seq(40,75,7)), maxit=10 ) 
#   summary(my[[xcomp]])
#   plot(my,whichplots=1)
#   
# }
               # beta= rbind( mx$beta, matrix(num.comp,num.comp,data=mx$lambda) ) ) #c(-8.62536e+02,-978.2655561,8.921929999,68.39673063,70.718308229) )
               #lambda = c(7.95685e-01,0.1092524,0.013272722,0.07049919,0.011290868),
               #sigma = c(1.13231e+00,8.1175685,0.031449944,0.29279219,0.391376814),
               
                            #rep(1,5), #c(6.54743e-01,0.7329752,0.040232035,-0.00127127,-0.000703873),
                            # mx$beta[2,],
               #              matrix(num.comp,num.comp,data=mx$lambda) ),
               # maxit=1 ) # c(-1.12561e-03,-0.0219165,0.000457071,0.00304349,-0.003454393) ) )
#plot(my,whichplots=2)

model.select <- function(ns){
  # sink(file = "gmm_model_selection.txt")
  outs<-data.frame(row.names = c("Components","LogLik","BIC","MSSE"))
  for( this.n in ns ){
    print( this.n )
    
    # eps <- 1e-6
    max.it <- 1000
    arbvar <- FALSE
    
    mx <- regmixEM(X.nz,Time.nz,verb = T,#epsilon=eps,
                   beta=rbind( rep(1418,this.n),0 ), maxit = max.it,arbvar = arbvar, arbmean = TRUE  )
    summary(mx)
    plot(mx,whichplots=1)
    # plot(mx,whichplots=2)
    sample.x <- sample.mixture.mx(mx,Time.nz,arbvar)
    
    hist(X.nz,breaks=100,main='Histogram of X locations',xlab='X',
         ylim=c(0,1),probability = TRUE)
    lines( density(sample.x), col='red' )
    
    my <- regmixEM(Y.nz,cbind(Time.nz,X.nz),verb=T,maxit = max.it,arbvar = arbvar,
                   beta=rbind(rep(65,this.n),0,tan((pi/180)*seq(0,180,length.out =this.n )) ), arbmean = TRUE )
    summary(my)
    plot(my,whichplots=1)
    
    sample.y <- sample.mixture.my(my,X.nz,Time.nz,arbvar)
    
    hist(Y.nz,breaks=100,main='Histogram of Y locations',xlab='Y',
         ylim=c(0,1), probability = TRUE)
    lines(density( sample.y ),col='red' )
    
    reconstruction(mx,my)
    
    # xs<-sample.mixture.mx(mx,Time.nz)
    # ys<-sample.mixture.my(my,xs,Time.nz)
    
    bounds.x <- range( range(sample.x), range(X.nz) )
    bounds.y <- range( range(sample.y), range(Y.nz) )
    
    plot(sample.x,sample.y,col='red',main="Location of Emergencies",xlab="X",ylab="Y",
         pch='x',cex=0.5,xlim=bounds.x,ylim=bounds.y)
    points(X.nz,Y.nz,cex=0.7)
    
    mean.sqrt.sse <- mean(sqrt( (X.nz-sample.x)*(X.nz-sample.x)+(Y.nz-sample.y)*(Y.nz-sample.y) ) )
    
    mix.bic <- function(modl){
      return( -2*modl$loglik+(length(modl$beta)+length(modl$lambda)+length(modl$sigma))*log(dim(modl$x)[1]))
    }
    outs <- rbind(outs,data.frame(Components=this.n,
                                  LogLik=round(mx$loglik+my$loglik,2), 
                                  BIC=mix.bic(mx)+mix.bic(my),
                                  MSSE=mean.sqrt.sse))
    # print(paste( this.n,  round(mx$loglik+my$loglik,2), mix.bic(mx)+mix.bic(my) ) )
  }
  print(outs)
}

guessed.model <- function(){
  # sink(file = "guessed_model.txt")
  max.it <- 1000
  
  arbvar <- FALSE
  
  start.x.int <- c(1400,1410,1415,1420)
  start.t.slope <- c(-1,1,1,1)
  start.y.int <- c(50,62,45,65)
  start.x.slope <- c(1,1,tan((5*pi)/12),0)
    
  mx <- regmixEM(y=X.nz,x=Time.nz,verb = T,#epsilon=eps,
                 beta=rbind( start.x.int, start.t.slope ), maxit = max.it , arbvar = arbvar, arbmean = TRUE )
  summary(mx)
  plot(mx,whichplots=1)
  # plot(mx,whichplots=2)
  
  hist(X.nz,breaks=100,main='Histogram of X locations',xlab='X',
       ylim=c(0,1),probability = TRUE)
  lines(density(sample.mixture.mx(mx,Time.nz) ), col='red' )
  
  my <- regmixEM(y=Y.nz,x=cbind(Time.nz,X.nz),verb=T,#epsilon = eps,
                 beta=rbind( start.y.int, start.t.slope, start.x.slope ), maxit = max.it , arbvar = arbvar, arbmean = TRUE )
  summary(my)
  plot(my,whichplots=1)
  
  hist(Y.nz,breaks=100,main='Histogram of Y locations',xlab='Y',
       ylim=c(0,1), probability = TRUE)
  lines(density(sample.mixture.my(my,X.nz,Time.nz) ),col='red' )
  
  reconstruction(mx,my)
  
  xs<-sample.mixture.mx(mx,Time.nz,arbvar)
  ys<-sample.mixture.my(my,xs,Time.nz,arbvar)
  
  bounds.x <- range( range(xs), range(X.nz) )
  bounds.y <- range( range(ys), range(Y.nz) )
  
  plot(xs,ys,col='red',main="Location of Emergencies",xlab="X",ylab="Y",
       pch='x',cex=0.5,xlim=bounds.x,ylim=bounds.y)
  points(X.nz,Y.nz,cex=0.7)
  
  mean.sqrt.sse <- mean(sqrt( (X.nz-xs)*(X.nz-xs)+(Y.nz-ys)*(Y.nz-ys) ) )
  
  mix.bic <- function(modl){
    return( -2*modl$loglik+(length(modl$beta)+length(modl$lambda)+length(modl$sigma))*log(dim(modl$x)[1]))
  }
  print( data.frame(Components=length(mx$lambda),
                    LogLik=round(mx$loglik+my$loglik,2), 
                    BIC=mix.bic(mx)+mix.bic(my),
                    MSSE=mean.sqrt.sse) ) 
}

bic <- function(modl,arbvar=TRUE){
  return( -2*modl$loglik+(length(modl$beta)+length(modl$lambda)+((1-arbvar)+arbvar*length(modl$sigma)) )*log(dim(modl$x)[1]))
}

model.select.x <- function(ns,xs,times,test.xs,test.times){
  sink(file = "x_gmm_model_selection.txt")
  outs<-data.frame(row.names = c("Components","LogLik","BIC","MSSE"))
  for( this.n in ns ){
    print( this.n )
    
    max.it <- 1000
    arbvar <- TRUE
    
    mx <- regmixEM(xs,times,verb = T,#epsilon=eps,
                   beta=rbind( rep(1418,this.n),0 ), maxit = max.it,
                   arbvar = arbvar, arbmean = TRUE  )
    print(summary(mx))
    plot(mx,whichplots=1)
    sample.x <- sample.mixture.mx(mx,times,arbvar)
    
    hist(xs,breaks=100,main='Histogram of X locations',xlab='X',
         ylim=c(0,1),probability = TRUE)
    lines( density(sample.x), col='red' )
    
    for( i in 1:length(mx$lambda) ){
      points( mx$beta[1,i], 0, col='red', pch=i )
    }
    
    legend.str <- paste(pasteCols( t(round( cbind(mx$lambda, mx$sigma),2)),sep="("),")",sep="" )
    legend("topright", legend=legend.str, pch=1:length(mx$lambda), col='red' ,cex=0.7)
    
    train.err <- mean(sqrt( (xs-sample.x)*(xs-sample.x) ) )
    
    sample.test.x <- sample.mixture.mx(mx,test.times,arbvar)
    test.err <- mean(sqrt( (test.xs-sample.test.x)*(test.xs-sample.test.x) ) )
    
    outs <- rbind(outs,data.frame(Components=this.n,
                                  LogLik=round(mx$loglik,2),
                                  BIC=bic(mx),
                                  training.error=train.err,
                                  testing.error=test.err))
    # print(paste( this.n,  round(mx$loglik+my$loglik,2), mix.bic(mx)+mix.bic(my) ) )
  }
  print(outs)
  return(mx)
}

model.select.y <- function(ns,ys,xs,times,test.ys,test.xs,test.times){
  sink(file = "y_gmm_model_selection.txt")
  outs<-data.frame(row.names = c("Components","LogLik","BIC","MSSE"))
  for( this.n in ns ){
    print( this.n )
    
    max.it <- 1000
    arbvar <- TRUE
    
    my <- regmixEM(ys,cbind(times,xs),verb=T,maxit = max.it,arbvar = arbvar,
                   beta=rbind(rep(65,this.n),0,tan((pi/180)*seq(0,180,length.out =this.n )) ), arbmean = TRUE )
    print(summary(my))
    plot(my,whichplots=1)
    
    sample.y <- sample.mixture.my(my,xs,times,arbvar)
    
    hist(ys,breaks=100,main='Histogram of Y locations',xlab='Y',
         ylim=c(0,1), probability = TRUE)
    lines(density( sample.y ),col='red' )
    
    for( i in 1:length(my$lambda) ){
      points( my$beta[1,i]+my$beta[3,i]*1418, 0, col='red', pch=i )
    }
    
    legend.str <- paste(pasteCols( t(round( cbind(my$lambda, my$sigma),2)),sep="("),")",sep="" )
    legend("topright", legend=legend.str, pch=1:length(my$lambda), col='red' ,cex=0.7)
    
    train.err <- mean(sqrt( (ys-sample.y)*(ys-sample.y) ) )
    
    test.sample.y <- sample.mixture.my(my,test.xs,test.times,arbvar)
    test.err <- mean(sqrt( (test.ys-test.sample.y)*(test.ys-test.sample.y) ) )
    
    outs <- rbind(outs,data.frame(Components=this.n,
                                  LogLik=round(my$loglik,2),
                                  BIC=bic(my),
                                  training.error=train.err,
                                  testing.error=test.err))
    # print(paste( this.n,  round(mx$loglik+my$loglik,2), mix.bic(mx)+mix.bic(my) ) )
  }
  print(outs)
  return(my)
}


jan_calls <- 'C:/Users/ashwi/workspace/RDDLv2/files/emergency_domain/XYT.csv'
big_calls <- 'C:/Users/ashwi/workspace/RDDLv2/files/emergency_domain/XYT_Big.csv'
callFile <- big_calls

calls <- read.csv( callFile )
attach(calls)

X.nz <- X[X!=0]
Y.nz <- Y[Y!=0]
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

#model.select.x(ns = 1:5,xs = train.data['x'][,1],times = train.data['t'][,1],
               #test.xs = test.data['x'][,1], test.times = test.data['t'][,1])

# model.select.y(ns = 1:5,ys = train.data['y'][,1],xs = train.data['x'][,1],times = train.data['t'][,1],
#                test.ys =test.data['y'][,1],test.xs = test.data['x'][,1], test.times = test.data['t'][,1])

selected.model <- function(){
  mx<-model.select.x(3,xs = train.data['x'][,1],times = train.data['t'][,1],
                 test.xs = test.data['x'][,1], test.times = test.data['t'][,1])
  my<-model.select.y(3,ys = train.data['y'][,1],xs = train.data['x'][,1],times = train.data['t'][,1],
                 test.ys =test.data['y'][,1],test.xs = test.data['x'][,1], test.times = test.data['t'][,1])
}