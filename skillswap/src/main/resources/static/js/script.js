// Profile Dropdown Menu Logic
const profileDropdownBtn = document.getElementById('profileDropdownBtn');
const profileDropdownMenu = document.getElementById('profileDropdownMenu');

if (profileDropdownBtn) {
    profileDropdownBtn.addEventListener('click', (event) => {
        event.stopPropagation(); // Prevents the window click event from firing immediately
        profileDropdownMenu.classList.toggle('hidden');
    });
}

window.addEventListener('click', (event) => {
    if (profileDropdownMenu && !profileDropdownBtn.contains(event.target)) {
        if (!profileDropdownMenu.classList.contains('hidden')) {
            profileDropdownMenu.classList.add('hidden');
        }
    }
});
// Hero Slider
const slides = document.querySelectorAll('.slide');
let currentSlide = 0;

function showSlide(i) {
  slides.forEach(s => s.classList.remove('active'));
  slides[i].classList.add('active');
}

function nextSlide() {
  currentSlide = (currentSlide + 1) % slides.length;
  showSlide(currentSlide);
}

setInterval(nextSlide, 5000);

// Mobile menu (future use)
document.getElementById('mobileMenuBtn').addEventListener('click', () => {
  // You can toggle mobile menu here
});

// About Us Modal
// About Us Modal Logic
const aboutModal = document.getElementById('aboutModal');
const aboutLink = document.getElementById('aboutLink');       // Navbar link
const aboutLinkFooter = document.getElementById('aboutLinkFooter'); // Footer link
const closeAbout = document.getElementById('closeAbout');

function openAboutModal() {
  aboutModal.classList.remove('hidden');
}

aboutLink.addEventListener('click', openAboutModal);
aboutLinkFooter.addEventListener('click', openAboutModal);

closeAbout.addEventListener('click', () => {
  aboutModal.classList.add('hidden');
});

aboutModal.addEventListener('click', (e) => {
  if (e.target === aboutModal) {
    aboutModal.classList.add('hidden');
  }
});


// FAQ Modal Logic
const faqModal = document.getElementById('faqModal');
const faqLink = document.getElementById('faqLink');
const closeFaq = document.getElementById('closeFaq');
const faqButtons = document.querySelectorAll('.faq-btn');

function openFaqModal() {
  faqModal.classList.remove('hidden');
}

// Add event listeners for the FAQ link
if (faqLink) {
  faqLink.addEventListener('click', openFaqModal);
}

// Add event listener to close the modal
if (closeFaq) {
  closeFaq.addEventListener('click', () => {
    faqModal.classList.add('hidden');
  });
}

// Add event listener for outside modal click
if (faqModal) {
    faqModal.addEventListener('click', (e) => {
        if (e.target === faqModal) {
            faqModal.classList.add('hidden');
        }
    });
}

// Collapsible FAQ answer logic
faqButtons.forEach(button => {
    button.addEventListener('click', () => {
        const answer = button.nextElementSibling;
        const icon = button.querySelector('.faq-icon');

        // Hide all other answers
        faqButtons.forEach(otherButton => {
            if (otherButton !== button) {
                otherButton.nextElementSibling.classList.add('hidden');
                otherButton.querySelector('.faq-icon').classList.remove('rotate-45');
            }
        });

        // Toggle the clicked answer
        answer.classList.toggle('hidden');
        icon.classList.toggle('rotate-45'); // Rotate the '+' icon
    });
});