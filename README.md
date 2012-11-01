Green-T Library
===============

An application-level library for battery life optimization of mobile devices running Android platform.

Purposes of the library
-----------------------

This library is intended to help you as a developer to improve the battery life of any device running any application that has the library integrated. Versions 2.3 and above of the Android platform are supported.

It is designed to be non-intrusive as long as your code is event-driven. The library enables your application with the following main features:

* Activates cellular data and/or Wi-Fi data when it is needed, and deactivates it when data transfer is no more necessary.
* Delegates geolocation to Wi-Fi where available. If not, it delegates geolocation to the GPS.

The features above can be activated or deactivated by configuration.

General Considerations
----------------------

As the library modifies system-wide settings, it may affect other applications running on the device. This has to be taken into account since Android currently doesn't support a way to change internal components behavior at an application level. And probably it doesn't make any sense at all.

Due to this limitation, the library may perfectly work for applications that are thought to be running in kiosk mode, in enterprise or public showroom environments.

Mobivery, in the context of the Green-T Project will continue to include new features in the library and will proceed to fix any bug found internally or by the Community, and will accept any pull request that fits the Green-T Project overall objectives.

Licensing
---------

This library is distributed under version 3.0 of the [GNU Lesser General Public License](http://www.gnu.org/copyleft/lesser.html). Please, feel free to use it under the terms of this license.