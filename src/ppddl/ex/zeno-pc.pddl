(define (domain zeno-travel)
  (:requirements :typing :negative-preconditions :universal-preconditions
		 :probabilistic-effects)
  (:types aircraft person city flevel - object)
  (:predicates (at ?x - (either person aircraft) ?c - city)
	       (boarding ?p - person ?a - aircraft)
	       (in ?p - person ?a - aircraft)
	       (debarking ?p -person ?a - aircraft)
	       (fuel-level ?a - aircraft ?l - flevel)
	       (next ?l1 ?l2 - flevel)
	       (flying ?a - aircraft ?c - city)
	       (zooming ?a - aircraft ?c - city)
	       (refueling ?a - aircraft))

  (:action start-boarding
	   :parameters (?p - person ?a - aircraft ?c - city)
	   :precondition (and (at ?p ?c) (at ?a ?c))
	   :effect (and (not (at ?p ?c)) (boarding ?p ?a)))

  (:action complete-boarding
	   :parameters (?p - person ?a - aircraft ?c - city)
	   :precondition (and (boarding ?p ?a) (at ?a ?c))
	   :effect (probabilistic 1/20 (and (not (boarding ?p ?a))
					    (in ?p ?a))))

  (:action start-debarking
	   :parameters (?p - person ?a - aircraft ?c - city)
	   :precondition (and (in ?p ?a) (at ?a ?c))
	   :effect (and (not (in ?p ?a)) (debarking ?p ?a)))

  (:action complete-debarking
	   :parameters (?p - person ?a - aircraft ?c - city)
	   :precondition (and (debarking ?p ?a) (at ?a ?c))
	   :effect (probabilistic 1/30 (and (not (debarking ?p ?a))
					    (at ?p ?c))))
  
  (:action start-flying
	   :parameters (?a - aircraft ?c1 ?c2 - city ?l1 ?l2 - flevel)
	   :precondition (and (at ?a ?c1) (fuel-level ?a ?l1) (next ?l2 ?l1)
			      (not (refueling ?a))
			      (forall (?p - person)
				      (and (not (boarding ?p ?a))
					   (not (debarking ?p ?a)))))
	   :effect (and (not (at ?a ?c1)) (flying ?a ?c2)))
                                  
  (:action complete-flying
	   :parameters (?a - aircraft ?c2 - city ?l1 ?l2 - flevel)
	   :precondition (and (flying ?a ?c2) (fuel-level ?a ?l1)
			      (next ?l2 ?l1))
	   :effect (probabilistic 1/180 (and (not (flying ?a ?c2)) (at ?a ?c2)
					     (not (fuel-level ?a ?l1))
					     (fuel-level ?a ?l2))))

  (:action start-zooming
	   :parameters (?a - aircraft ?c1 ?c2 - city ?l1 ?l2 ?l3 - flevel)
	   :precondition (and (at ?a ?c1) (fuel-level ?a ?l1) (next ?l2 ?l1)
			      (next ?l3 ?l2) (not (refueling ?a))
			      (forall (?p - person)
				      (and (not (boarding ?p ?a))
					   (not (debarking ?p ?a)))))
	   :effect (and (not (at ?a ?c1)) (zooming ?a ?c2)))
                                  
  (:action complete-zooming
	   :parameters (?a - aircraft ?c2 - city ?l1 ?l2 ?l3 - flevel)
	   :precondition (and (zooming ?a ?c2) (fuel-level ?a ?l1)
			      (next ?l2 ?l1) (next ?l3 ?l2))
	   :effect (probabilistic 1/100 (and (not (zooming ?a ?c2)) (at ?a ?c2)
					     (not (fuel-level ?a ?l1))
					     (fuel-level ?a ?l3))))

  (:action start-refueling
	   :parameters (?a - aircraft ?c - city ?l ?l1 - flevel)
	   :precondition (and (at ?a ?c) (not (refueling ?a))
			      (fuel-level ?a ?l) (next ?l ?l1))
	   :effect (refueling ?a))

  (:action complete-refuling
	   :parameters (?a - aircraft ?l ?l1 - flevel)
	   :precondition (and (refueling ?a) (fuel-level ?a ?l) (next ?l ?l1))
	   :effect (probabilistic 1/73 (and (not (refueling ?a))
					    (fuel-level ?a ?l1)
					    (not (fuel-level ?a ?l))))))


(define (problem ZTRAVEL-1-2)
(:domain zeno-travel)
(:objects
	plane1 - aircraft
	person1 - person
	person2 - person
	city0 - city
	city1 - city
	city2 - city
	fl0 - flevel
	fl1 - flevel
	fl2 - flevel
	fl3 - flevel
	fl4 - flevel
	fl5 - flevel
	fl6 - flevel
	)
(:init
	(at plane1 city0)
	(fuel-level plane1 fl1)
	(at person1 city0)
	(at person2 city2)
	(next fl0 fl1)
	(next fl1 fl2)
	(next fl2 fl3)
	(next fl3 fl4)
	(next fl4 fl5)
	(next fl5 fl6)
)
(:goal (and
	(at plane1 city1)
	(at person1 city0)
	(at person2 city2)
	))
)
