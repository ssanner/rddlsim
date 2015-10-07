#!/usr/bin/python
""" Setup script for the Gurobi optimizer
"""

# This script installs the gurobi module in your local environment, allowing
# you to say 'import gurobipy' from the Python shell.
#
# To install the Gurobi libraries, type 'python setup.py install'.  You
# may need to run this as superuser on a Linux system.
#
# We are grateful to Stuart Mitchell for his help with this script.

from distutils.core import setup #, Extension
import os,sys

License = """
    This software is covered by the Gurobi End User License Agreement.
    By completing the Gurobi installation process and using the software,
    you are accepting the terms of this agreement.
"""
if sys.version_info[0:2] != (2, 7) and sys.version_info[0:2] != (3, 2) and sys.version_info[0:2] != (3, 4):
  raise RuntimeError("Unsupported Python version")

if os.name == 'posix':
  if sys.platform == 'darwin':
    srcpath = os.path.join('lib', 'gurobipy')
  else:
    version = 'python'+str(sys.version_info[0])+'.'+str(sys.version_info[1])
    if sys.maxunicode <= 1<<16:
      version = version+'_utf16'
    else:
      version = version+'_utf32'
    srcpath = os.path.join('lib', version, 'gurobipy')
  srcfile = 'gurobipy.so'
elif os.name == 'nt':
  version = 'python'+str(sys.version_info[0])+str(sys.version_info[1])
  srcpath = os.path.join(version, 'lib', 'gurobipy')
  srcfile = 'gurobipy.pyd'
else:
  raise RuntimeError("Unsupported operating system")

setup(name="gurobipy",
      version="6.0.5",
      description="""
    The Gurobi optimization engines represent the next generation in
    high-performance optimization software.
    """,
      license = License,
      url="http://www.gurobi.com/",
      author="Gurobi Optimization, Inc.",
      packages = ['gurobipy'],
      package_dir={'gurobipy' : srcpath },
      package_data = {'gurobipy' : [srcfile] }
      )

if sys.platform == 'darwin':
  import platform,subprocess
  from distutils.sysconfig import get_python_lib
  sitelib = get_python_lib() + '/gurobipy/gurobipy.so'
  if platform.mac_ver()[0] >= "10.11": # El Capitan
    import glob,distutils.spawn
    GUROBI_HOME = os.sep.join(os.readlink(distutils.spawn.find_executable("gurobi_cl")).split(os.sep)[:-2])
    SHARED_LIB = glob.glob(GUROBI_HOME+"/lib/libgurobi*.so")[0].split(os.sep)[-1]
    subprocess.call(('install_name_tool', '-change',
                     SHARED_LIB, GUROBI_HOME + "/lib/" + SHARED_LIB, sitelib))
  default = '/System/Library/Frameworks/Python.framework/Versions/2.7/Python'
  modified = sys.prefix + '/Python'
  if default != modified:
    import os.path
    if not os.path.isfile(modified):
      modified = sys.prefix + '/lib/libpython2.7.dylib' # For Anaconda
    subprocess.call(('install_name_tool', '-change', default, modified, sitelib))
