#!/usr/bin/env python

from setuptools import setup, find_packages

version = '0.9'
setup(name='foresite',
      version=version,
      description='Library for constructing, parsing, manipulating and serializing OAI-ORE Resource Maps',
      long_description="""\
""",
      classifiers=[],
      author='Rob Sanderson',
      author_email='azaroth@liv.ac.uk',
      url='http://code.google.com/p/foresite-toolkit/',
      license='BSD',
      packages=find_packages(exclude=['ez_setup', 'examples', 'tests']),
      include_package_data=True,
      zip_safe=False,
      install_requires=['rdflib<3a', 'lxml'],
      test_suite='foresite.tests.test_suite'
      )

