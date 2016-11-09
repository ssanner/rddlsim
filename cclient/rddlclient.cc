/*
 * Copyright 2003-2005 Carnegie Mellon University and Rutgers University
 * Copyright 2007 Hakan Younes
 * Copyright 2011 Sungwook Yoon, Scott Sanner (modified for RDDLSim)
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

/***********************************************************************
 * Note: the procedures in this file go from least important (top) to  *
 * most important (bottom), so read in reverse to get a sense of       *
 * control flow.                                                       *
 ***********************************************************************/

/***********************************************************************/
/*                           INCLUDES / DEFS                           */
/***********************************************************************/

#include "strxml.h"

/** Ubuntu wants the following two libraries **/
#include <stdlib.h>
#include <string.h>
/**********************************************/

#include <string>
#include <iostream>
#include <cerrno>
#include <cstdio>
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>
#include <netinet/in.h>

#if HAVE_SSTREAM
#include <sstream>
#else
#include <strstream>
namespace std {
typedef std::ostrstream ostringstream;
}
#endif
#include <unistd.h>

/***********************************************************************/
/*                RDDLSIM SERVER INTERACTION HELPER PROCS              */
/***********************************************************************/

/* Extracts a fluent from the given XML node. */
static int getFluent(const XMLNode* atomNode, std::string& fluent) {

  if (atomNode == 0 || atomNode->getName() != "observed-fluent") {
    return -1;
  }

  // Get fluent name
  if (!atomNode->dissect("fluent-name", fluent)) {
    return -1;
  }

  // Get fluent arguments and value
  fluent += "(";
  int arg_count = 0;
  bool value;
  for (int i = 0; i < atomNode->size(); i++) {
    const XMLNode* termNode = atomNode->getChild(i);
    if (termNode == 0) {
      continue;

    } else if (termNode->getName() == "fluent-arg") {
      if (arg_count++ > 0)
	fluent += ",";
      fluent += termNode->getText();

    } else if ( termNode != 0 && termNode->getName() == "fluent-value") {
      value = termNode->getText() == "true";
    }
  }
  fluent += ")";

  return (int)value; // Should be -1:error, 0:false, 1:true
}

/* Extracts a state (multiple fluents/values) from the given XML node. */
static bool showState(const XMLNode* stateNode) {
  if (stateNode == 0) {
    return false;
  }
  if (stateNode->getName() != "turn") {
    return false;
  }

  std::cout << "==============================================\n" << std::endl;

  if (stateNode->size() == 2 && 
      stateNode->getChild(1)->getName() == "no-observed-fluents") {

    // The first turn for a POMDP will have this null observation
    std::cout << "No state/observations received.\n" << std::endl;

  } else {
    
    // Show all state or observation fluents for this turn
    std::cout << "True state/observation variables:" << std::endl;
    for (int i = 0; i < stateNode->size(); i++) {
      const XMLNode* cn = stateNode->getChild(i);
      if (cn->getName() == "observed-fluent") {
	std::string fluent;
	int val = getFluent(cn, fluent);

	// Only display true fluents
	if (val)
	  std::cout << "- " << fluent << std::endl;
      } 
    }
    std::cout << std::endl;
  }

  return true;
}

/* Sends an action on the given stream. */
void sendAction(std::ostream& os) {

  // A simple 'noop'
  //std::cout << "--> Action taken: noop\n" << std::endl;
  //os << "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" 
  //   << "<actions/>" << '\0';

  // A more complex action example specific to 'SysAdmin'
  int comp_num = (rand() % 4) + 1;
  std::cout << "--> Action taken: reboot(c" << comp_num << ")\n" << std::endl;
  os << "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" 
     << "<actions><action>"
     << "<action-name>reboot</action-name>"
     << "<action-arg>c" << comp_num << "</action-arg>"
     << "<action-value>true</action-value>"
     << "</action></actions>" << '\0';

  // Example for a domain with three *concurrent* single-argument actions 
  //os << "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" 
  //   << "<actions>"
  //   << "<action><action-name>advance</action-name><action-arg>i1</action-arg><action-value>true</action-value></action>"
  //   << "<action><action-name>advance</action-name><action-arg>i2</action-arg><action-value>true</action-value></action>"
  //   << "<action><action-name>advance</action-name><action-arg>i3</action-arg><action-value>true</action-value></action>"
  //   << "</actions>" << '\0';
}

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

/* helper connect function */
int connectToServer(const char *hostname, int port)
{
  struct hostent *host = ::gethostbyname(hostname);
  if (!host) {
    perror("gethostbyname");
    return -1;
  }

  int sock = ::socket(PF_INET, SOCK_STREAM, 0);
  if (sock == -1) {
    perror("socket");
    return -1;
  }

  struct sockaddr_in addr;
  addr.sin_family=AF_INET;
  addr.sin_port=htons(port);
  addr.sin_addr = *((struct in_addr *)host->h_addr);
  memset(&(addr.sin_zero), '\0', 8);

  if (::connect(sock, (struct sockaddr*)&addr, sizeof(addr)) == -1) {
    perror("connect");
    return -1;
  }
  return sock;
  //remember to call close(sock) when you're done
}

/***********************************************************************/
/*                     MAIN SERVER INTERACTION LOOP                    */
/***********************************************************************/

/* Constructs an XML client and actually runs all of the server interaction*/
void doMainClientLoop(const std::string& instance_name,
                      const std::string& client_name, 
                      int fd) {
  std::ostringstream os;
  os.str("");
  os << "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" << "<session-request>"
	 <<  "<problem-name>" << instance_name << "</problem-name>"
     <<  "<client-name>" << client_name << "</client-name>"
     <<  "<no-header/>"
     << "</session-request>"
     << '\0';
#if !HAVE_SSTREAM
  os << '\0';
#endif
  write(fd, os.str().c_str(), os.str().length());

  const XMLNode* sessionInitNode = read_node(fd);

  int total_rounds;
  long round_time;
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

  // Do a round
  int rounds_count = 0;
  while (++rounds_count <= total_rounds) {

    std::cout << "***********************************************" << std::endl;
    std::cout << ">>> ROUND INIT " << rounds_count << "/" << total_rounds << "; time remaining = " << round_time << std::endl;
    std::cout << "***********************************************" << std::endl;

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
	
	std::string s;
	if (response->dissect("round-reward", s)) {
	  float reward = atof(s.c_str());
	  std::cout << "***********************************************" << std::endl;
	  std::cout << ">>> END OF ROUND -- REWARD RECEIVED: " << reward << std::endl;
	  std::cout << "***********************************************\n" << std::endl;
	}

        //std::cout << response << std::endl;
        break;
      }

      // Display the state / observations
      // TODO: Read this into your planner's internal representation
      if (!showState(response)) {
	std::cerr << "Invalid state response: " << response << std::endl;
        delete response;
        return;
      }

      // Send an action
      // TODO: Based on the state above, your planner chooses the action
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

    std::string s;
    if (endSessionNode->dissect("total-reward", s)) {
      float reward = atof(s.c_str());
      std::cout << "***********************************************" << std::endl;
      std::cout << ">>> END OF SESSION -- OVERALL REWARD: " << reward << std::endl;
      std::cout << "***********************************************\n" << std::endl;
    }

    //std::cout << endSessionNode << std::endl;
    delete endSessionNode;
  }
}

/***********************************************************************/
/*                      MAIN ENTRY POINT TO CLIENT                     */
/***********************************************************************/

int main(int argc, char **argv)
{
  if (argc != 2) {
    /* note: actions are currently hard-coded for SysAdmin */
    std::cout << "\nusage: rddlclient [sysm1|sysm2|sysp1|sysp2]" << std::endl;
    exit(1);
  }
  std::string instance_request = argv[1];

  /* we hardcode most arguments, better to read from command line **/
  std::string host("localhost");
  std::string client_name("your-client-name");
  int port = 2323;
  try {

    int socket = connectToServer(host.c_str(), port);
    if (socket <= 0) {
      std::cerr << "Could not connect to " << host << ':' << port << std::endl;
      return 1;
    }

    doMainClientLoop(instance_request, client_name, socket);

  } catch (const std::exception& e) {
    std::cerr << std::endl << "mdpclient: " << e.what() << std::endl;
    return 1;
  } catch (...) {
    std::cerr << "mdpclient: fatal error" << std::endl;
    return -1;
  }

  return 0;
}
