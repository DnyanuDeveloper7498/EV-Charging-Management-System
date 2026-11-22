// Loads Razorpay Checkout once. Only the public key id (from the backend) is ever used in the browser.
export function loadRazorpay() {
  return new Promise((resolve, reject) => {
    if (window.Razorpay) {
      resolve(window.Razorpay);
      return;
    }
    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.onload = () => resolve(window.Razorpay);
    script.onerror = () =>
      reject(
        new Error(
          "Could not load Razorpay Checkout. Please check your internet connection.",
        ),
      );
    document.body.appendChild(script);
  });
}
