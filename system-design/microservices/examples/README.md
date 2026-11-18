# Tiny Shop Microservices Lab

Original, dependency-free **Node.js 22+** example—not copied from the supplied Spring Cloud project. It provides a consistent scenario for the microservices textbook.

## Start

~~~bash
node shop-lab.mjs
~~~

In another terminal:

~~~bash
curl http://127.0.0.1:4100/api/products/7
curl http://127.0.0.1:4100/api/users/9
curl -X POST 'http://127.0.0.1:4100/api/orders?userId=9&productId=7'
curl http://127.0.0.1:4100/api/orders
curl -X POST 'http://127.0.0.1:4100/api/orders?userId=9&productId=7&slow=1'
~~~

The last request responds with 503 because Product is delayed 800 ms while Order uses a 350 ms timeout. A successful order emits a local, in-process order.placed event. The two Product instances (ports 4101 and 4104) are selected round-robin in the Order Service.

## Components

- Gateway 4100, Order Service 4103, User Service 4102, Product instances 4101 and 4104.
- All services expose /health.
- Demonstrates service boundaries, HTTP calls, basic gateway routing, instance selection, timeouts, trace IDs, and one local event.

## Boundaries

This file does **not** implement a production registry, distributed queue, circuit breaker, authentication, durable storage, service discovery, distributed trace collector or dynamic configuration. These topics are explained in the textbook, not falsely represented as built.

All servers run in one OS process for easy learning. Splitting into real deployments would introduce actual process and network boundaries. NestJS code fragments in chapters are illustrations requiring a NestJS project.

**Study principle:** keep the shop example; extend only when a new system-design concept requires it.