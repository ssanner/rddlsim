/*
 * Copyright 2003-2005 Carnegie Mellon University and Rutgers University
 * Copyright 2007 H�kan Younes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include "config.h"
#include "client.h"
#include "strxml.h"
#if HAVE_SSTREAM
#include <sstream>
#else
#include <strstream>
namespace std {
typedef std::ostrstream ostringstream;
}
#endif
#include <unistd.h>


/* Extracts session request information. */
static bool sessionRequestInfo(const XMLNode* node,
                               int& rounds, long& time) {

  if (node == 0) {
    return false;
  }

  std::string s;
  if (!node->dissect("num-rounds", s)) {
    return false;
  }
  rounds = atoi(s.c_str());

  if (!node->dissect("time-allowed", s)) {
    return false;
  }
  time = atol(s.c_str());

  return true;
}


/* Extracts an action from the given XML node. */
static void getAtom(const std::string& problem, const XMLNode* atomNode) {

//
//  if (atomNode == 0 || atomNode->getName() != "observed-fluent") {
//    return 0;
//  }
//  std::string predicate_name;
//  if (!atomNode->dissect("fluent-name", predicate_name)) {
//    return 0;
//  }
//
////  const Predicate* p =
////    problem.domain().predicates().find_predicate(predicate_name);
////  if (p == 0) {
////    return 0;
////  }
//
//  predicate_name += "_";
//  TermList terms;
////  size_t argIndex = 0;
//  bool value;
//  for (int i = 0; i < atomNode->size(); i++) {
//    const XMLNode* termNode = atomNode->getChild(i);
//    if (termNode == 0 || termNode->getName() != "fluent-arg") {
//    	if ( termNode != 0 && termNode->getName() == "fluent-value") {
//    		value = termNode->getText() == "true";
//    	}
//      continue;
//    }
////    if (argIndex >= PredicateTable::parameters(*p).size()) {
////      return 0;
////    }
////    Type correctType = PredicateTable::parameters(*p)[argIndex];
////    argIndex++;
////
////    std::string term_name = termNode->getText();
////    const Object* o = problem.terms().find_object(term_name);
////    if (o != 0) {
////      if (!TypeTable::subtype(TermTable::type(*o), correctType)) {
////        return 0;
////      }
////    } else {
////      o = problem.domain().terms().find_object(term_name);
////      if (o == 0) {
////        return 0;
////      } else if (!TypeTable::subtype(TermTable::type(*o), correctType)) {
////        return 0;
////      }
////    }
////    terms.push_back(*o);
//
//    predicate_name += "_";
//    predicate_name += termNode->getText();
//
//  }
////  std::cout << predicate_name  << " value " << value << std::endl;
//  const Predicate* p =
//      problem.domain().predicates().find_predicate(predicate_name);
//
////  if (PredicateTable::parameters(*p).size() != terms.size()) {
////    return 0;
////  }
//
//  if ( p == 0 ) {
//	  return 0;
//  }
//
//  return &Atom::make(*p, terms);
	return;
}


/* Extracts a fluent from the given XML node. */
static void getFluent(const std::string& problem,
                               const XMLNode* appNode) {
//  if (appNode == 0 || appNode->getName() != "fluent") {
//    return 0;
//  }
//
//  std::string function_name;
//  if (!appNode->dissect("function", function_name)) {
//    return 0;
//  }
//  const Function* f =
//    problem.domain().functions().find_function(function_name);
//  if (f == 0) {
//    return 0;
//  }
//
//  TermList terms;
//  size_t argIndex = 0;
//  for (int i = 0; i<appNode->size(); i++) {
//    const XMLNode* termNode = appNode->getChild(i);
//    if (!termNode || termNode->getName() != "term") {
//      continue;
//    }
//    if (argIndex >= FunctionTable::parameters(*f).size()) {
//      return 0;
//    }
//    Type correctType = FunctionTable::parameters(*f)[argIndex];
//    argIndex++;
//
//    std::string term_name = termNode->getText();
//    const Object* o = problem.terms().find_object(term_name);
//    if (o != 0) {
//      if (!TypeTable::subtype(TermTable::type(*o), correctType)) {
//        return 0;
//      }
//    } else {
//      o = problem.domain().terms().find_object(term_name);
//      if (o == 0) {
//        return 0;
//      }
//      else if (!TypeTable::subtype(TermTable::type(*o), correctType)) {
//        return 0;
//      }
//    }
//
//    terms.push_back(*o);
//  }
//
//  if (FunctionTable::parameters(*f).size() != terms.size()) {
//    return 0;
//  }
//
//  return &Fluent::make(*f,terms);
}


/* Extracts a state from the given XML node. */
static bool getState(const std::string& problem, const XMLNode* stateNode) {
//  if (stateNode == 0) {
//    return false;
//  }
//  if (stateNode->getName() != "turn") {
//    return false;
//  }
//  for (int i = 0; i < stateNode->size(); i++) {
//    const XMLNode* cn = stateNode->getChild(i);
//    if (cn->getName() == "observed-fluent") {
//      const Atom* atom = getAtom(problem, cn);
//      if (atom != 0) {
//        atoms.insert(atom);
//        RCObject::ref(atom);
//      }
//    }
//    else if (cn->getName() == "fluent") {
//      const Fluent* fluent = getFluent(problem, cn);
//      std::string value_str;
//      if (!cn->dissect("value", value_str))
//        return false;
//      values.insert(std::make_pair(fluent, Rational(value_str.c_str())));
//      RCObject::ref(fluent);
//    }
//  }

  return true;
}


/* Sends an action on the given stream. */
static void sendAction(std::ostream& os) {
//  if (action == 0) {
//    os << "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" << "<done/>" << '\0';
//  } else if (action->name() == "noop") {
	  std::cout << "noop" << std::endl;
	  os << "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" << "<actions/>" << '\0';
//  } else {
////	    os << "<act>" << "<action><name>" << action->name() << "</name>";
////	    for (ObjectList::const_iterator oi = action->arguments().begin();
////	         oi != action->arguments().end(); oi++) {
////	      os << "<term>" << *oi << "</term>";
////	    }
////	    os << "</action></act>";
//	std::cout << action->name() << std::endl;
//	std::string action_name;
//	action_name = action->name();
//	std::string::size_type position = action_name.find("__");
//	if ( position < action_name.length( )) {
//		os << "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" << "<actions>" << "<action><action-name>" <<
//				action_name.substr(0, position) << "</action-name>";
//		position += 2;
//		while(1) {
//			std::string::size_type new_position = action_name.find("_", position);
//			if ( new_position > action_name.length( )) {
//				break;
//			}
//			os << "<action-arg>" << action_name.substr(position, (new_position - position)) << "</action-arg>";
//			position = new_position +1;
//		}
//		if (position < action_name.length()) {
//			os << "<action-arg>" << action_name.substr(position) << "</action-arg>";
//		}
//	} else {
//		os << "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" << "<actions>" << "<action><action-name>" << action_name << "</action-name>";
//	}
//	os << "<action-value>true</action-value></action></actions>" << '\0';
//  }
}


/* ====================================================================== */
/* XMLClient */

/* Constructs an XML client */
XMLClient::XMLClient(const std::string& problemName,
                     const std::string& name, int fd, int horizon) {
  std::ostringstream os;
  os.str("");
  os << "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" << "<session-request>"
	 <<  "<problem-name>" << problemName << "</problem-name>"
     <<  "<client-name>" << name << "</client-name>"
     <<  "<no-header/>"
     << "</session-request>"
     << '\0';
#if !HAVE_SSTREAM
  os << '\0';
#endif
  write(fd, os.str().c_str(), os.str().length());

  const XMLNode* sessionInitNode = read_node(fd);

  int total_rounds, round_turns;
  long round_time;
  round_turns = horizon;
  if (!sessionRequestInfo(sessionInitNode,
                          total_rounds, round_time)) {
    std::cerr << "Error in server's session-request response" << std::endl;
    if (sessionInitNode != 0) {
      delete sessionInitNode;
    }
    return;
  }

  if (sessionInitNode != 0) {
    delete sessionInitNode;
  }

  int rounds_left = total_rounds;
  while (rounds_left) {
    rounds_left--;
    os.str("");
    os << "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" <<  "<round-request/>" << '\0';
#if !HAVE_SSTREAM
    os << '\0';
#endif
    write(fd, os.str().c_str(), os.str().length());
    const XMLNode* roundInitNode = read_node(fd);
    if (!roundInitNode || roundInitNode->getName() != "round-init") {
      std::cerr << "Error in server's round-request response" << std::endl;
      if (roundInitNode != 0) {
        delete roundInitNode;
      }
      return;
    }

    delete roundInitNode;
    const XMLNode* response = 0;
    while (1) {

      if (response != 0) {
        delete response;
      }

      response = read_node(fd);

      if (!response) {
        std::cerr << "Invalid state response" << std::endl;
        return;
      }

      if (response->getName() == "round-end"
          || response->getName() == "session-end") {
        std::cout << response << std::endl;
        break;
      }


//      AtomSet atoms;
//      ValueMap values;
//      if (!getState(atoms, values, problem, response)) {
//        std::cerr << "Invalid state response: " << response << std::endl;
//        delete response;
//        return;
//      }
//
//      const Action *a = planner.decideAction(atoms, values);
//      for (AtomSet::const_iterator ai = atoms.begin();
//           ai != atoms.end(); ai++) {
//        RCObject::destructive_deref(*ai);
//      }
//      for (ValueMap::const_iterator vi = values.begin();
//           vi != values.end(); vi++) {
//        RCObject::destructive_deref((*vi).first);
//      }

      os.str("");
      sendAction(os);
#if !HAVE_SSTREAM
      os << '\0';
#endif
      write(fd, os.str().c_str(), os.str().length());
    }


    if (response && response->getName() == "end-session") {
      delete response;
      break;
    }
    if (response != 0) {
      delete response;
    }
  }
  const XMLNode* endSessionNode = read_node(fd);

  if (endSessionNode) {
    std::cout << endSessionNode << std::endl;
    delete endSessionNode;
  }
}
