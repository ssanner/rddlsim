;; BoxWorld domain from BRP 2001

(define (domain brp2001-bw)
 (:requirements :typing :equality :disjunctive-preconditions
                :probabilistic-effects :existential-preconditions
                :conditional-effects :negative-preconditions
                :universal-preconditions :rewards)
 (:types city box truck)
 (:constants paris - city )
 (:predicates (rain)
              (bin ?b - box ?c - city)
              (tin ?t - truck ?c - city)
              (on ?b - box ?t - truck))
 (:action noop)
 (:action load
  :parameters (?b - box ?t - truck)
  :precondition (exists (?c - city) 
			(and (bin ?b ?c)
                             (tin ?t ?c)))
  :effect (and (when (not (rain))
                  (probabilistic 0.9 (and (on ?b ?t)
                                     (forall (?c - city) (not (bin ?b ?c))))))
               (when (rain)
                  (probabilistic 0.7 (and (on ?b ?t)
                                     (forall (?c - city) (not (bin ?b ?c)))))))
 )
 (:action unload
  :parameters (?b - box ?t - truck)
  :precondition (on ?b ?t)
  :effect (probabilistic 0.99 (and (not (on ?b ?t))
                                   (forall (?c - city)
                                      (when (tin ?t ?c)
                                            (bin ?b ?c)))))
 )
 (:action drive
  :parameters (?t - truck ?dst - city)
  :precondition (not (tin ?t ?dst))
  :effect (probabilistic 0.99 (and (forall (?src - city)
                                      (when (tin ?t ?src)
                                            (not (tin ?t ?src))))
                                   (tin ?t ?dst)))
 )
)

(define
 (problem brp2001-bw-p0)
  (:domain brp2001-bw)
  (:objects box0 - box
            box1 - box
            truck0 - truck
            city0 - city
  )
  (:init (bin box0 city0)
         (bin box1 paris)
         (tin truck0 paris)
  )
  (:goal (exists (?b - box) (bin ?b paris)))
  (:goal-reward 500)
)

(define
 (problem brp2001-bw-p1)
  (:domain brp2001-bw)
  (:objects box0 - box
            box1 - box
            truck0 - truck
            city0 - city
  )
  (:init (bin box0 city0)
         (tin truck0 paris)
         (on  box1 truck0)
  )
  (:goal (exists (?b - box) (bin ?b paris)))
  (:goal-reward 500)
)

(define
 (problem brp2001-bw-p2)
  (:domain brp2001-bw)
  (:objects box0 - box
            box1 - box
            truck0 - truck
            city0 - city
  )
  (:init (bin box0 city0)
         (tin truck0 city0)
         (on  box1 truck0)
  )
  (:goal (exists (?b - box) (bin ?b paris)))
  (:goal-reward 500)
)

(define
 (problem brp2001-bw-p3)
  (:domain brp2001-bw)
  (:objects box0 - box
            box1 - box
            truck0 - truck
            city0 - city
  )
  (:init (bin box0 city0)
         (tin truck0 city0)
         (bin box1 city0)
  )
  (:goal (exists (?b - box) (bin ?b paris)))
  (:goal-reward 500)
)

(define
 (problem brp2001-bw-p4)
  (:domain brp2001-bw)
  (:objects box0 - box
            box1 - box
            truck0 - truck
            city0 - city
	    city1 - city
  )
  (:init (bin box0 city1)
         (tin truck0 city0)
         (bin box1 city1)
  )
  (:goal (exists (?b - box) (bin ?b paris)))
  (:goal-reward 500)
)