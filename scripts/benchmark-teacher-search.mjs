#!/usr/bin/env node

const baseUrl = process.env.TEACHER_SEARCH_URL
  ?? "http://localhost:8080/api/public/teachers?keyword=Toan&page=0&size=20";
const requestsPerLevel = Number(process.env.REQUESTS_PER_LEVEL ?? 500);
const concurrencyLevels = (process.env.CONCURRENCY_LEVELS ?? "25,50,80,100")
  .split(",")
  .map(Number);

if (!Number.isInteger(requestsPerLevel) || requestsPerLevel < 1
    || concurrencyLevels.some((value) => !Number.isInteger(value) || value < 1)) {
  throw new Error("REQUESTS_PER_LEVEL and CONCURRENCY_LEVELS must contain positive integers");
}

function percentile(sortedValues, percentileValue) {
  const index = Math.min(
    sortedValues.length - 1,
    Math.ceil((percentileValue / 100) * sortedValues.length) - 1,
  );
  return sortedValues[Math.max(0, index)];
}

async function runLevel(concurrency) {
  const latencies = [];
  const errors = [];
  let nextRequest = 0;
  const startedAt = performance.now();

  async function worker() {
    while (true) {
      const requestNumber = nextRequest++;
      if (requestNumber >= requestsPerLevel) return;

      const requestStartedAt = performance.now();
      try {
        const response = await fetch(baseUrl, { cache: "no-store" });
        await response.arrayBuffer();
        if (!response.ok) errors.push(`HTTP ${response.status}`);
      } catch (error) {
        errors.push(error instanceof Error ? error.message : String(error));
      } finally {
        latencies.push(performance.now() - requestStartedAt);
      }
    }
  }

  await Promise.all(Array.from({ length: concurrency }, worker));
  latencies.sort((left, right) => left - right);
  const elapsedSeconds = (performance.now() - startedAt) / 1000;

  return {
    concurrency,
    requests: requestsPerLevel,
    errors: errors.length,
    p50Ms: Number(percentile(latencies, 50).toFixed(1)),
    p95Ms: Number(percentile(latencies, 95).toFixed(1)),
    requestsPerSecond: Number((requestsPerLevel / elapsedSeconds).toFixed(1)),
  };
}

console.log(`Target: ${baseUrl}`);
console.log("For a cold-cache run, start Spring with SPRING_CACHE_TYPE=none.");
for (const concurrency of concurrencyLevels) {
  console.table([await runLevel(concurrency)]);
}
