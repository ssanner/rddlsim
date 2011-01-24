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
#include <config.h>
#include "client.h"
#include <iostream>
#include <cerrno>
#include <cstdio>
#if HAVE_GETOPT_LONG
#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
#include <getopt.h>
#else
#include "port/getopt.h"
#endif
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>
#include <netinet/in.h>

std::string current_file;
/* Level of warnings. */
int warning_level;
/* Verbosity level. */
int verbosity;

/* Program options. */
static struct option long_options[] = {
  { "host", required_argument, 0, 'H' },
  { "port", required_argument, 0, 'P' },
  { "verbose", optional_argument, 0, 'v' },
  { "warnings", optional_argument, 0, 'W' },
  { "help", no_argument, 0, 'h' },
  { "horizon", required_argument, 0, 'z' },
  { 0, 0, 0, 0 }
};
static const char OPTION_STRING[] = "H:P:v::W::h:z";


/* Displays help. */
static void display_help() {
  std::cout << "usage: mdpclient [options] [file ...]" << std::endl
            << "options:" << std::endl
            << "  -H h,  --host=h\t"
            << "connect to host h" << std::endl
            << "  -P p,  --port=p\t"
            << "connect to port p" << std::endl
            << "  -v[n], --verbose[=n]\t"
            << "use verbosity level n;" << std::endl
            << "\t\t\t  n is a number from 0 (verbose mode off) and up;"
            << std::endl
            << "\t\t\t  default level is 1 if optional argument is left out"
            << std::endl
            << "  -W[n], --warnings[=n]\t"
            << "determines how warnings are treated;" << std::endl
            << "\t\t\t  0 supresses warnings; 1 displays warnings;"
            << std::endl
            << "\t\t\t  2 treats warnings as errors" << std::endl
            << "  -h     --help\t\t"
            << "display this help and exit" << std::endl
            << "  file ...\t\t"
            << "files containing domain and problem descriptions;" << std::endl
            << "\t\t\t  if none, descriptions are read from standard input"
            << std::endl
            << std::endl
            << "Report bugs to <" PACKAGE_BUGREPORT ">." << std::endl;
}


int connect(const char *hostname, int port)
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

class RandomPlanner : public Planner
{
 public:
  RandomPlanner(const std::string& problem) : Planner(problem) { srand(time(0)); }
  virtual void initRound();
  virtual ~RandomPlanner() {}
  virtual void endRound();
};

void RandomPlanner::initRound()
{

}

void RandomPlanner::endRound()
{

}

int main(int argc, char **argv)
{
  /* Set default verbosity. */
  verbosity = 0;
  /* Set default warning level. */
  warning_level = 1;
  /* Host. */
  std::string host;
  /* Port. */
  int port = 0;
  int horizon = 10;
  try {
    /*
     * Get command line options.
     */
    while (1) {
      int option_index = 0;
      int c = getopt_long(argc, argv, OPTION_STRING,
                          long_options, &option_index);
      if (c == -1) {
        break;
      }
      switch (c) {
      case 'H':
        host = optarg;
        break;
      case 'P':
        port = atoi(optarg);
        break;
      case 'v':
        verbosity = (optarg != 0) ? atoi(optarg) : 1;
        break;
      case 'W':
        warning_level = (optarg != 0) ? atoi(optarg) : 1;
        break;
      case 'h':
        display_help();
        return 0;
      case 'z':
    	  horizon = atoi(optarg);
    	  break;
      default:
        std::cerr << "Try `mdpclient --help' for more information."
                  << std::endl;
        return -1;
      }
    }

    int socket = connect(host.c_str(), port);
    if (socket <= 0) {
      std::cerr << "Could not connect to " << host << ':' << port << std::endl;
      return 1;
    }

    XMLClient(argv[optind++], "johnclient", socket, horizon);

  } catch (const std::exception& e) {
    std::cerr << std::endl << "mdpclient: " << e.what() << std::endl;
    return 1;
  } catch (...) {
    std::cerr << "mdpclient: fatal error" << std::endl;
    return -1;
  }

  return 0;
}
