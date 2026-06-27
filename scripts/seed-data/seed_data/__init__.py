"""Production-like data seeder for a local Buurman workspace.

Creates users, teams, properties, contacts, contracts, payments, documents and
photos through the public REST API, simulating N landlords using Buurman over M
months. Signups follow a startup growth curve; per-user activity follows a normal
distribution. The only direct-DB action is backdating historical timestamps the
API cannot set (see ``backdate.py``).
"""

__version__ = "0.1.0"
